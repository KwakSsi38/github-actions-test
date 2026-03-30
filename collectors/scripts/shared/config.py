"""
shared/config.py — 파이프라인 공통 설정

prompts, ai_info 양쪽에서 공유하는 OCI 접속 정보, HTTP 설정을 관리.
각 파이프라인 전용 설정은 각자의 config.py에서 추가로 정의.
"""

import os

# ── OCI ───────────────────────────────────────────────────────────────────────
OCI_NAMESPACE  = os.environ.get("OCI_NAMESPACE", "")
OCI_BUCKET     = os.environ.get("OCI_BUCKET", "")
UPLOAD_RETRIES = 3
UPLOAD_DELAY   = 2.0

# ── HTTP ──────────────────────────────────────────────────────────────────────
REQUEST_TIMEOUT_SECONDS = 30
MAX_RETRY_ATTEMPTS      = 3
RETRY_WAIT_MIN_SECONDS  = 4
RETRY_WAIT_MAX_SECONDS  = 30

# ── 로깅 ──────────────────────────────────────────────────────────────────────
LOG_FORMAT      = "%(asctime)s [%(levelname)s] %(name)s — %(message)s"
LOG_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"