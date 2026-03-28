"""
prompts/enrich.py — GitHub REST API로 레포 메타데이터 수집

- ETag 기반 변경 감지 (변경된 레포만 갱신)
- private / fork 레포 제외
- enrich 완료 시 content_pending 큐에 추가
- github_id를 primary key로 사용 (owner/repo 변경 대응)
"""

import json
import logging
import time
from datetime import datetime, timezone
from pathlib import Path

from collectors.scripts.prompts.config import SKIP_FORKS
from collectors.scripts.shared.github_client import BASE_DELAY, github_get, rotator
from collectors.scripts.shared.utils import now_iso, save_json

logger = logging.getLogger(__name__)


# ── 파싱 ──────────────────────────────────────────────────────────────────────
def _parse_repo(data: dict) -> dict:
    owner = data.get("owner", {})
    lic   = data.get("license")
    return {
        "github_id":         data["id"],
        "name":              data.get("name"),
        "source_repo":       data.get("full_name"),
        "source_url":        data.get("html_url"),
        "summary":           data.get("description"),
        "track":             None,
        "star_count":        data.get("stargazers_count"),
        "fork_count":        data.get("forks_count"),
        "size":              data.get("size"),
        "language_stats":    None,
        "license":           lic.get("spdx_id") if lic else None,
        "homepage":          data.get("homepage") or None,
        "owner_avatar_url":  owner.get("avatar_url"),
        "owner_type":        owner.get("type", "").upper() or None,
        "is_official":       None,
        "default_branch":    data.get("default_branch"),
        "etag":              None,
        "source_updated_at": data.get("updated_at"),
        "active":            not data.get("archived", False),
        "raw_metadata": {
            "topics":      data.get("topics", []),
            "visibility":  data.get("visibility"),
            "pushed_at":   data.get("pushed_at"),
            "created_at":  data.get("created_at"),
            "is_fork":     data.get("fork"),
            "is_archived": data.get("archived"),
        },
    }


def _fetch_languages(source_repo: str) -> dict | None:
    status, body, _ = github_get(f"https://api.github.com/repos/{source_repo}/languages")
    time.sleep(BASE_DELAY)
    if status == 200 and body:
        total = sum(body.values()) or 1
        return {lang: round(b / total * 100, 1) for lang, b in body.items()}
    return None


def _make_filename(github_id: int, source_repo: str) -> str:
    owner, repo = source_repo.split("/", 1)
    return f"{github_id}_{owner}_{repo}.json"


def _ensure_content_pending(index: dict, gid_str: str) -> None:
    """content_pending 큐에 중복 없이 추가."""
    q = index["queue"]["content_pending"]
    if gid_str not in q:
        q.append(gid_str)
        index["repos"][gid_str]["content_status"] = "pending"


# ── 단일 레포 enrich ───────────────────────────────────────────────────────────
def enrich_one(
    source_repo:     str,
    file_entries:    list[dict],
    index:           dict,
    work_dir:        Path,
    stored_etag:     str | None = None,
    existing_skills: list | None = None,
) -> dict | None:
    """
    레포 1개를 enrich하고 work_dir에 JSON 저장.
    index의 queue / repos를 직접 수정.

    반환:
      {"github_id": str, "filename": str, "changed": bool} — 성공
      None — 스킵 또는 실패
    """
    extra  = {"If-None-Match": stored_etag} if stored_etag else {}
    status, body, resp_headers = github_get(
        f"https://api.github.com/repos/{source_repo}", extra_headers=extra
    )
    time.sleep(BASE_DELAY)

    # ── 304: 변경 없음 ────────────────────────────────────────────────────────
    if status == 304:
        indexed = {meta["source_repo"]: gid for gid, meta in index["repos"].items()}
        gid_str = indexed.get(source_repo)
        if gid_str:
            _ensure_content_pending(index, gid_str)
        logger.info("  → 변경 없음 (304): %s", source_repo)
        return {
            "github_id": gid_str,
            "filename":  index["repos"].get(gid_str, {}).get("filename"),
            "changed":   False,
        }

    # ── 404: 삭제 / 이동 ──────────────────────────────────────────────────────
    if status == 404:
        logger.warning("  → 404 (삭제/이동): %s", source_repo)
        indexed = {meta["source_repo"]: gid for gid, meta in index["repos"].items()}
        gid_str = indexed.get(source_repo)
        if gid_str:
            index["repos"][gid_str].update({
                "active":         False,
                "enrich_status":  "done",
                "content_status": "none",
            })
        return None

    if status != 200 or body is None:
        logger.warning("  → HTTP %d: %s", status, source_repo)
        return None

    # ── private / fork 제외 ───────────────────────────────────────────────────
    if body.get("private"):
        logger.info("  → private 레포 — 스킵: %s", source_repo)
        return None
    if SKIP_FORKS and body.get("fork"):
        logger.info("  → fork 레포 — 스킵: %s", source_repo)
        return None

    # ── 메타데이터 파싱 ───────────────────────────────────────────────────────
    repo_meta                   = _parse_repo(body)
    repo_meta["etag"]           = resp_headers.get("ETag")
    repo_meta["language_stats"] = _fetch_languages(repo_meta["source_repo"])

    github_id  = repo_meta["github_id"]
    gid_str    = str(github_id)
    new_source = repo_meta["source_repo"]
    filename   = _make_filename(github_id, new_source)

    if new_source != source_repo:
        logger.info("  → 레포 이동 감지: %s → %s", source_repo, new_source)

    # ── skills 구성 (기존 content_md / content_hash 유지) ─────────────────────
    existing_map = {s["file_path"]: s for s in (existing_skills or [])}
    skills = [
        {
            "skill_name":   fp.strip("/").split("/")[-2] if "/" in fp.strip("/") else "(root)",
            "file_path":    fp,
            "content_md":   existing_map.get(fp, {}).get("content_md"),
            "content_hash": existing_map.get(fp, {}).get("content_hash"),
            "raw_metadata": existing_map.get(fp, {}).get("raw_metadata"),
        }
        for fe in file_entries
        for fp in [fe["file_path"]]
    ]

    # ── work_dir에 JSON 저장 (원자적) ─────────────────────────────────────────
    work_dir.mkdir(exist_ok=True)
    save_json(
        {"repository": repo_meta, "skills": skills, "agent": None},
        work_dir / filename,
    )

    # ── index 갱신 ────────────────────────────────────────────────────────────
    now = now_iso()
    index["repos"][gid_str] = {
        "source_repo":    new_source,
        "filename":       filename,
        "etag":           repo_meta["etag"],
        "oci_etag":       None,
        "active":         repo_meta["active"],
        "enrich_status":  "done",
        "content_status": "pending",
        "last_enriched":  now,
        "last_content":   index["repos"].get(gid_str, {}).get("last_content"),
    }
    _ensure_content_pending(index, gid_str)

    return {"github_id": gid_str, "filename": filename, "changed": True}
