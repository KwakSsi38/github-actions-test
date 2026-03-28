"""
merge_model_info.py — 모델 정보 + 벤치마크 통합

Artificial Analysis(벤치마크)와 OpenRouter(모델 메타데이터)의 데이터를
모델 slug 기반으로 매칭하여 벤더 → 패밀리 → 모델 계층 구조로 통합.

출력 구조:
    [
      {
        "name": "Anthropic",
        "families": [
          {
            "family_name": "CLAUDE-3.5",
            "models": [ { "model_name": ..., "api_id": ..., ... } ]
          }
        ]
      }
    ]
"""

import logging
import sys

from collectors.scripts.ai_info.config import AI_INFO_DIR, RANKINGS_DIR, VENDOR_OFFICIAL_URLS
from collectors.scripts.ai_info.utils import (
    current_timestamp,
    extract_family_name,
    load_json,
    normalize_for_match,
    save_json,
    setup_logging,
)

logger = logging.getLogger(__name__)

AA_INPUT_FILE = RANKINGS_DIR / "models_benchmark_raw.json"
OR_INPUT_FILE = AI_INFO_DIR  / "filtered_major_models.json"
OUTPUT_FILE   = AI_INFO_DIR  / "integrated_major_models.json"


# ── 매칭 맵 생성 ──────────────────────────────────────────────────────────────
def build_or_match_map(or_items: list[dict]) -> dict[str, dict]:
    match_map: dict[str, dict] = {}
    for model in or_items:
        key = normalize_for_match(model.get("id", ""))
        if key in match_map:
            logger.warning("중복 정규화 키: '%s' (id: %s)", key, model.get("id"))
        match_map[key] = model
    return match_map


def find_or_match(aa_model: dict, or_match_map: dict[str, dict]) -> dict | None:
    """slug 매칭 → 실패 시 이름으로 재시도."""
    slug_key = normalize_for_match(aa_model.get("slug", ""))
    if slug_key in or_match_map:
        return or_match_map[slug_key]

    name_key = normalize_for_match(aa_model.get("name", ""))
    if name_key in or_match_map:
        logger.debug("slug 매칭 실패, 이름으로 재매칭: %s", aa_model.get("name"))
        return or_match_map[name_key]

    return None


# ── 벤더 / 패밀리 구조 빌더 ───────────────────────────────────────────────────
def get_or_create_vendor(vendor_groups: dict, vendor_name: str, vendor_slug: str) -> dict:
    if vendor_name not in vendor_groups:
        official_url = next(
            (url for kw, url in VENDOR_OFFICIAL_URLS.items() if kw in vendor_slug),
            None,
        )
        if not official_url:
            logger.warning("벤더 '%s'의 공식 URL을 찾을 수 없습니다.", vendor_name)
            official_url = ""

        vendor_groups[vendor_name] = {
            "name":          vendor_name,
            "official_url":  official_url,
            "is_active":     True,
            "is_deprecated": False,
            "families":      {},
        }
    return vendor_groups[vendor_name]


def get_or_create_family(vendor: dict, family_name: str, vendor_name: str, created_at: str) -> dict:
    families = vendor["families"]
    if family_name not in families:
        families[family_name] = {
            "family_name":        family_name,
            "common_description": f"{vendor_name}의 {family_name} 시리즈 모델입니다.",
            "created_at":         created_at,
            "models":             [],
        }
    return families[family_name]


def build_model_entry(aa_model: dict, or_match: dict) -> dict:
    arch         = or_match.get("architecture", {})
    top_provider = or_match.get("top_provider") or {}
    aa_name      = aa_model.get("name", "")
    creator_slug = aa_model.get("model_creator", {}).get("slug", "")

    return {
        "model_name":        aa_name,
        "api_id":            or_match.get("id"),
        "context_window":    or_match.get("context_length", 0),
        "max_output_tokens": top_provider.get("max_completion_tokens") or 0,
        "release_date":      aa_model.get("release_date"),
        "is_preview":        any(kw in aa_name.lower() for kw in ("preview", "beta")),
        "model_image_url":   f"https://img.service.com/logos/{creator_slug}.png",
        "input_price":       aa_model.get("pricing", {}).get("price_1m_input_tokens"),
        "output_price":      aa_model.get("pricing", {}).get("price_1m_output_tokens"),
        "input_modalities":  arch.get("input_modalities", ["text"]),
        "output_modalities": arch.get("output_modalities", ["text"]),
        "category":          "General",
    }


# ── 통합 실행 ─────────────────────────────────────────────────────────────────
def merge(aa_data: dict, or_data: dict) -> list[dict]:
    or_match_map: dict[str, dict] = build_or_match_map(or_data.get("data", []))
    vendor_groups: dict[str, dict] = {}
    unmatched: list[str] = []
    matched_count = 0
    created_at    = current_timestamp()

    for aa_model in aa_data.get("data", []):
        or_match = find_or_match(aa_model, or_match_map)
        if not or_match:
            unmatched.append(aa_model.get("slug", "unknown"))
            continue

        creator     = aa_model.get("model_creator", {})
        vendor_name = creator.get("name", "Unknown")
        vendor_slug = creator.get("slug", "").lower()

        vendor      = get_or_create_vendor(vendor_groups, vendor_name, vendor_slug)
        family_name = extract_family_name(aa_model.get("name", ""))
        family      = get_or_create_family(vendor, family_name, vendor_name, created_at)
        family["models"].append(build_model_entry(aa_model, or_match))
        matched_count += 1

    if unmatched:
        logger.warning(
            "매칭 실패 %d개 (%.1f%%): %s",
            len(unmatched),
            len(unmatched) / max(len(aa_data.get("data", [])), 1) * 100,
            unmatched[:10],
        )

    logger.info("매칭 성공: %d개 모델", matched_count)

    result = []
    for v_data in vendor_groups.values():
        v_data["families"] = list(v_data["families"].values())
        result.append(v_data)

    return result


def main() -> None:
    setup_logging()
    logger.info("모델 정보 + 벤치마크 통합 시작")

    aa_data    = load_json(AA_INPUT_FILE)
    or_data    = load_json(OR_INPUT_FILE)
    integrated = merge(aa_data, or_data)

    if not integrated:
        logger.error("통합 결과가 비어있습니다. 매칭 로직을 확인하세요.")
        sys.exit(1)

    save_json(integrated, OUTPUT_FILE)
    logger.info("통합 완료: %d개 벤더 저장 → %s", len(integrated), OUTPUT_FILE)


if __name__ == "__main__":
    main()
