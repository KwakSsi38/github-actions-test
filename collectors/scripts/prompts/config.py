"""
prompts/config.py — prompts 파이프라인 전용 설정
"""

import os
from pathlib import Path

# ── OCI 경로 ──────────────────────────────────────────────────────────────────
OCI_DATA_PREFIX   = "data/prompts/"
OCI_INDEX_OBJECT  = "data/prompts/config/index.json"
OCI_LOCK_OBJECT   = "data/prompts/config/lock.json"
LOCK_TTL_SEC      = 6 * 60 * 60  # 6시간

# ── 스케줄러 설정 ─────────────────────────────────────────────────────────────
CHECKPOINT_N    = 100             # N개마다 index.json 중간 저장
TIMEOUT_MARGIN  = 20              # Actions 종료 N분 전에 자동 중단
MAX_RUNTIME_SEC = (340 - TIMEOUT_MARGIN) * 60
WORK_DIR        = Path("./work")

# ── Sourcegraph ───────────────────────────────────────────────────────────────
SOURCEGRAPH_TOKEN  = os.environ.get("SOURCEGRAPH_TOKEN", "")
SOURCEGRAPH_URL    = "https://sourcegraph.com/.api/search/stream"
SOURCEGRAPH_QUERY  = r"file:(?i)skill\.md count:all"

# ── GitHub ────────────────────────────────────────────────────────────────────
SKIP_FORKS      = os.environ.get("SKIP_FORKS", "true").lower() == "true"
SIZE_LIMIT      = 1_000_000   # 1MB 초과 시 raw URL fallback
MIN_CONTENT_LEN = 10          # 최소 콘텐츠 길이 (바이트)

# ── Discord 알림 ──────────────────────────────────────────────────────────────
FAIL_ALERT_THRESHOLD = int(os.environ.get("FAIL_ALERT_THRESHOLD", "50"))

# ── 민감 정보 마스킹 대상 ─────────────────────────────────────────────────────
SENSITIVE_VALUES = [
    v for v in [
        os.environ.get("GITHUB_TOKEN_1"),
        os.environ.get("GITHUB_TOKEN_2"),
        os.environ.get("GITHUB_TOKEN_3"),
        os.environ.get("SOURCEGRAPH_TOKEN"),
        os.environ.get("DISCORD_WEBHOOK_URL"),
        os.environ.get("OCI_NAMESPACE"),
        os.environ.get("OCI_BUCKET"),
    ] if v
]
