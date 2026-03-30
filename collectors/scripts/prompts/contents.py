"""
prompts/contents.py — GitHub Contents API로 SKILL.md 내용 수집

- SHA 기반 파일 단위 변경 감지 (동일 sha면 재수집 스킵)
- 1MB 초과 파일: download_url(raw) fallback + 토큰 인증
- 빈 파일 / 최소 길이 미달 필터링
- 인코딩 오류 감지 및 raw_metadata 플래그 기록
"""

import base64
import logging
import time
from pathlib import Path

import requests

from collectors.scripts.prompts.config import MIN_CONTENT_LEN, SIZE_LIMIT
from collectors.scripts.shared.github_client import BASE_DELAY, github_get, rotator
from collectors.scripts.shared.utils import save_json, load_json

logger = logging.getLogger(__name__)


# ── 디코딩 ────────────────────────────────────────────────────────────────────
def _decode(raw_bytes: bytes) -> tuple[str, bool]:
    """(decoded_str, has_encoding_error) 반환."""
    try:
        return raw_bytes.decode("utf-8"), False
    except UnicodeDecodeError:
        return raw_bytes.decode("utf-8", errors="replace"), True


# ── raw URL fallback ──────────────────────────────────────────────────────────
def _get_raw(download_url: str) -> str | None:
    for attempt in range(3):
        try:
            resp = requests.get(
                download_url, headers=rotator.raw_headers(), timeout=30
            )
            if resp.status_code == 200:
                return resp.text
            if resp.status_code in (404, 403):
                return None
        except requests.exceptions.RequestException as e:
            logger.error("raw 수집 오류 (시도 %d): %s", attempt + 1, e)
            time.sleep(5)
    return None


# ── Contents API 호출 ─────────────────────────────────────────────────────────
def _get_content(
    source_repo: str, file_path: str, branch: str
) -> tuple[str, str, bool] | None:
    """
    (content, git_blob_sha, has_encoding_error) 반환.
    1MB 초과 시 raw URL fallback. 실패 시 None.
    """
    url = f"https://api.github.com/repos/{source_repo}/contents/{file_path}"

    for attempt in range(3):
        try:
            resp = requests.get(
                url,
                headers=rotator.headers(),
                params={"ref": branch},
                timeout=30,
            )

            # rate limit 처리 (github_client의 로직과 동일하게) # TODO: 이건 그냥 동일하게가 아니라 가져다 쓰면 되는 거 아닌가? TM-135
            remaining = int(resp.headers.get("X-RateLimit-Remaining", 9999))
            reset_at  = int(resp.headers.get("X-RateLimit-Reset", 0))
            if resp.status_code == 403 and remaining == 0:
                import time as _time
                wait = max(reset_at - int(_time.time()), 0) + 5
                logger.warning("Rate limit 소진 — %d초 대기", wait)
                _time.sleep(wait)
                rotator.rotate()
                continue
            if resp.status_code == 429:
                wait = int(resp.headers.get("Retry-After", 60))
                logger.warning("429 Too Many Requests — %d초 대기", wait)
                time.sleep(wait)
                continue
            if remaining < 100:
                time.sleep(3.0)
            elif remaining < 500:
                time.sleep(1.5)

            if resp.status_code == 200:
                body         = resp.json()
                sha          = body.get("sha", "")
                size         = body.get("size", 0)
                encoded      = body.get("content", "").replace("\n", "")
                download_url = body.get("download_url", "")

                if size > SIZE_LIMIT or not encoded:
                    logger.info("  → %d bytes — raw fallback: %s", size, file_path)
                    content = _get_raw(download_url)
                    if content is None:
                        return None
                    return content, sha, False

                content, has_err = _decode(base64.b64decode(encoded))
                if has_err:
                    logger.warning("  → 인코딩 오류: %s", file_path)
                return content, sha, has_err

            if resp.status_code in (404, 403):
                return None

            logger.warning("Contents API HTTP %d (시도 %d)", resp.status_code, attempt + 1)

        except requests.exceptions.RequestException as e:
            logger.error("Contents API 오류 (시도 %d): %s", attempt + 1, e)
            time.sleep(5)

    return None


# ── 단일 레포 처리 ────────────────────────────────────────────────────────────
def fetch_one(gid_str: str, index: dict, work_dir: Path) -> bool:
    # TODO: 단일 레포에 700개 이렇게 있는 경우도 있는데 이런 건 어떻게 처리해야 할지? 100개마다 저장할 수 있게 하는 게 낫지 않을까? TM-135
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

    for skill in skills:
        file_path  = skill["file_path"]
        stored_sha = skill.get("content_hash")

        result = _get_content(source_repo, file_path, branch)
        time.sleep(BASE_DELAY)

        if result is None: # TODO: 수집 실패 이유 명시 필요 (TM-135)
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