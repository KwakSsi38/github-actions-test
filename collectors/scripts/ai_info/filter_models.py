"""
filter_models.py — 주요 벤더 모델 필터링

fetch_or_models에서 수집한 전체 모델 목록에서
OpenAI / Anthropic / Google 모델만 추출.
모델 ID에 벤더 키워드가 포함된 항목만 선별.
"""

import logging
import sys

from collectors.scripts.ai_info.config import AI_INFO_DIR, TARGET_VENDORS
from collectors.scripts.ai_info.utils import load_json, save_json, setup_logging

logger = logging.getLogger(__name__)

INPUT_FILE  = AI_INFO_DIR / "models_info_raw.json"
OUTPUT_FILE = AI_INFO_DIR / "filtered_major_models.json"


def filter_by_vendor(model_list: list[dict], vendors: list[str]) -> list[dict]:
    """모델 ID에 벤더 키워드가 포함된 항목만 반환."""
    return [
        model for model in model_list
        if any(vendor in model.get("id", "").lower() for vendor in vendors)
    ]


def main() -> None:
    setup_logging()
    logger.info("주요 벤더 모델 필터링 시작 (대상 벤더: %s)", TARGET_VENDORS)

    raw_data   = load_json(INPUT_FILE)
    model_list = raw_data.get("data", [])
    logger.info("전체 모델 수: %d개", len(model_list))

    filtered = filter_by_vendor(model_list, TARGET_VENDORS)

    if not filtered:
        logger.error("조건에 맞는 모델이 없습니다. 필터 키워드를 확인하세요.")
        sys.exit(1)

    save_json({"data": filtered}, OUTPUT_FILE)
    logger.info("필터링 완료: %d개 모델 저장 → %s", len(filtered), OUTPUT_FILE)


if __name__ == "__main__":
    main()
