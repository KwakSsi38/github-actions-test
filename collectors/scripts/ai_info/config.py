"""
config.py — ai_info 파이프라인 설정

raw 데이터 수집 + OCI 업로드만 담당.
가공/매칭/통계는 Java(Spring)에서 처리.
"""

import os
from pathlib import Path

# ── 프로젝트 경로 ──────────────────────────────────────────────────────────────
ROOT_DIR     = Path(__file__).resolve().parents[3]
DATA_DIR     = ROOT_DIR / "data"
AI_INFO_DIR  = DATA_DIR / "ai-info"
RANKINGS_DIR = DATA_DIR / "rankings"

# ── API 엔드포인트 ─────────────────────────────────────────────────────────────
OPENROUTER_MODELS_URL   = "https://openrouter.ai/api/v1/models"
ARTIFICIAL_ANALYSIS_URL = "https://artificialanalysis.ai/api/v2/data/llms/models"

# ── HTTP 설정 ──────────────────────────────────────────────────────────────────
REQUEST_TIMEOUT_SECONDS = 30
MAX_RETRY_ATTEMPTS      = 3
RETRY_WAIT_MIN_SECONDS  = 4
RETRY_WAIT_MAX_SECONDS  = 30

# ── OCI 설정 ───────────────────────────────────────────────────────────────────
from collectors.scripts.shared.config import OCI_NAMESPACE, OCI_BUCKET  # noqa

OCI_PREFIX = "data/ai-info/"

# OCI 업로드 대상: raw 데이터만 (가공은 Java에서 처리)
OCI_UPLOAD_TARGETS: list[tuple[Path, str]] = [
    (AI_INFO_DIR  / "models_info_raw.json",      f"{OCI_PREFIX}models_info_raw.json"),
    (RANKINGS_DIR / "models_benchmark_raw.json", f"{OCI_PREFIX}models_benchmark_raw.json"),
]