"""
prompts/sourcegraph.py — Sourcegraph Stream API로 SKILL.md 포함 레포 목록 수집
"""

import json
import logging
import re
import sys
from collections import defaultdict

import requests

from collectors.scripts.prompts.config import (
    SOURCEGRAPH_QUERY,
    SOURCEGRAPH_TOKEN,
    SOURCEGRAPH_URL,
)

logger = logging.getLogger(__name__)


def collect_repos() -> dict[str, list[dict]]:
    """
    반환: { "owner/repo": [{"file_path": "path/to/SKILL.md"}, ...] }
    실패 시 sys.exit(1).
    """
    if not SOURCEGRAPH_TOKEN:
        logger.error("SOURCEGRAPH_TOKEN 환경변수가 설정되지 않았습니다.")
        sys.exit(1)

    headers = {
        "Authorization": f"token {SOURCEGRAPH_TOKEN}",
        "Accept":        "text/event-stream",
    }
    results: dict[str, list] = defaultdict(list)
    seen: set[str]            = set()

    logger.info("Sourcegraph 쿼리: %s", SOURCEGRAPH_QUERY)

    try:
        with requests.get(
            SOURCEGRAPH_URL,
            headers=headers,
            params={"q": SOURCEGRAPH_QUERY, "v": "V3"},
            stream=True,
            timeout=180,
        ) as resp:
            if resp.status_code != 200:
                raise RuntimeError(
                    f"Sourcegraph HTTP {resp.status_code}: {resp.text[:200]}"
                )

            event_type = None
            for raw_line in resp.iter_lines(decode_unicode=True):
                if not raw_line:
                    event_type = None
                    continue
                if raw_line.startswith("event:"):
                    event_type = raw_line[len("event:"):].strip()
                    continue
                if raw_line.startswith("data:") and event_type == "matches":
                    try:
                        matches = json.loads(raw_line[len("data:"):].strip())
                        for m in matches:
                            if m.get("type") != "path":
                                continue
                            source_repo = re.sub(r"^github\.com/", "", m.get("repository", ""))
                            file_path   = m.get("path", "")
                            key         = f"{source_repo}::{file_path}"
                            if key in seen or not source_repo:
                                continue
                            seen.add(key)
                            results[source_repo].append({"file_path": file_path})
                    except json.JSONDecodeError:
                        pass

    except requests.exceptions.RequestException as e:
        logger.error("Sourcegraph 요청 오류: %s", e)
        raise

    logger.info("수집 완료 — %d개 레포 / %d개 파일", len(results), len(seen))
    return dict(results)
