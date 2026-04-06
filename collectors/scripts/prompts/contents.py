import base64
import logging
import time
from pathlib import Path

import requests

from collectors.scripts.prompts.config import MIN_CONTENT_LEN, SIZE_LIMIT
from collectors.scripts.shared.github_client import BASE_DELAY, github_get, rotator
from collectors.scripts.shared.utils import save_json, load_json

logger = logging.getLogger(__name__)

# ... (상단 _decode, _get_raw, _get_content 함수는 기존과 동일하게 유지) ...

def fetch_one(gid_str: str, index: dict, work_dir: Path) -> str:
    """
    반환값: "updated", "skipped", "failed" 중 하나
    """
    meta = index["repos"].get(gid_str)
    if not meta:
        logger.warning("index에 없는 github_id: %s", gid_str)
        return "failed"

    source_repo = meta["source_repo"]
    filename    = meta["filename"]
    local_path  = work_dir / filename

    if not local_path.exists():
        logger.warning("로컬 파일 없음: %s", filename)
        return "failed"

    data   = load_json(local_path)
    branch = data.get("repository", {}).get("default_branch", "main")
    skills = data.get("skills", [])

    logger.info("  %s — %d개 skill 확인", source_repo, len(skills))

    # [임시 방어 로직] 1000개 초과 시 스킵
    if len(skills) > 1000:
        logger.warning("    → SKILL.md 개수 초과 (%d개 > 1000개). 수집 스킵.", len(skills))
        return "skipped"

    changed = False
    for skill in skills:
        file_path  = skill["file_path"]
        stored_sha = skill.get("content_hash")

        result = _get_content(source_repo, file_path, branch)
        time.sleep(BASE_DELAY)

        if result is None:
            logger.warning("    → 수집 실패: %s", file_path)
            continue

        content, new_sha, has_err = result

        if stored_sha and stored_sha == new_sha:
            logger.debug("    → SHA 동일, 스킵: %s", file_path)
            continue

        if not content or len(content.strip()) < MIN_CONTENT_LEN:
            logger.info("    → 내용 없음/너무 짧음, 스킵: %s", file_path)
            continue

        skill["content_md"]   = content
        skill["content_hash"] = new_sha
        if skill.get("raw_metadata") is None:
            skill["raw_metadata"] = {}
        skill["raw_metadata"]["has_encoding_error"] = has_err
        changed = True

    # 변경된 내용이 없다면 OCI 재업로드를 막기 위해 skipped 반환
    if not changed:
        return "skipped"

    save_json(data, local_path)
    return "updated"