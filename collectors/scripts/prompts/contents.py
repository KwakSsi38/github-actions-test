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

def fetch_one(gid_str: str, index: dict, work_dir: Path) -> bool:
    """
    content_pending 큐의 레포 1개를 처리.
    work_dir의 JSON을 읽어 skill 내용을 채운 뒤 원자적으로 덮어씀.
    성공 시 True, 실패 시 False.
    """
    meta = index["repos"].get(gid_str)
    if not meta:
        logger.warning("index에 없는 github_id: %s", gid_str)
        return False

    source_repo = meta["source_repo"]
    filename    = meta["filename"]
    local_path  = work_dir / filename

    if not local_path.exists():
        logger.warning("로컬 파일 없음: %s", filename)
        return False

    data   = load_json(local_path)
    branch = data.get("repository", {}).get("default_branch", "main")
    skills = data.get("skills", [])

    logger.info("  %s — %d개 skill 확인", source_repo, len(skills))

    # [임시 방어 로직] GitHub Actions 환경의 타임아웃 및 리소스 한계를 고려하여,
    # 단일 레포에서 수집해야 할 SKILL.md가 1000개를 초과하면 해당 레포 수집을 통째로 스킵합니다.
    # TODO: 추후 파이프라인을 Docker 환경으로 이식하여 리소스 제약이 풀리면 이 제한을 해제할 예정.
    if len(skills) > 1000:
        logger.warning("    → SKILL.md 개수 초과 (%d개 > 1000개). 수집 스킵.", len(skills))
        # 스킵하더라도 실패(무한 재시도)로 빠지지 않도록 처리 완료로 마킹 (True 반환)
        return True

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

    # 원자적 저장
    save_json(data, local_path)
    return True