"""
config.py — 모든 설정값 중앙 관리

경로, API 엔드포인트, 필터 키워드, HTTP 설정, OCI 업로드 대상을 한 곳에서 관리.
하드코딩 값을 스크립트에 직접 쓰지 않고 이 파일에서 import해서 사용.
"""

import os
from pathlib import Path

# ── 프로젝트 경로 ──────────────────────────────────────────────────────────────
ROOT_DIR     = Path(__file__).resolve().parents[3]
DATA_DIR     = ROOT_DIR / "data"
AI_INFO_DIR  = DATA_DIR / "ai-info"
RANKINGS_DIR = DATA_DIR / "rankings"

# ── API 엔드포인트 ─────────────────────────────────────────────────────────────
OPENROUTER_MODELS_URL    = "https://openrouter.ai/api/v1/models"
ARTIFICIAL_ANALYSIS_URL  = "https://artificialanalysis.ai/api/v2/data/llms/models"

# ── 필터링 대상 벤더 ───────────────────────────────────────────────────────────
TARGET_VENDORS: list[str] = ["openai", "anthropic", "google"]

VENDOR_OFFICIAL_URLS: dict[str, str] = {
    "openai":    "https://platform.openai.com/docs",
    "anthropic": "https://platform.claude.com/docs/ko/home",
    "google":    "https://ai.google.dev/",
}

# ── 패밀리 정규화에서 제거할 등급/크기 키워드 ─────────────────────────────────
TIER_KEYWORDS: set[str] = {
    "mini", "nano", "pro", "lite", "flash", "edge", "preview", "beta",
    "instruct", "chat", "free", "plus", "small", "medium", "large", "max",
    "non-reasoning", "high", "low",
}

# ── 벤치마크 추출 메트릭 정의 ─────────────────────────────────────────────────
# (metric_type, dot_path, unit)
BENCHMARK_METRICS: list[tuple[str, str, str]] = [
    ("INTELLIGENCE",  "evaluations.artificial_analysis_intelligence_index", "points"),
    ("CODING",        "evaluations.artificial_analysis_coding_index",       "points"),
    ("MATH",          "evaluations.artificial_analysis_math_index",         "points"),
    ("TPS",           "median_output_tokens_per_second",                    "tokens/sec"),
    ("TTFT",          "median_time_to_first_token_seconds",                 "sec"),
    ("PRICE_BLENDED", "pricing.price_1m_blended_3_to_1",                   "$"),
]

# ── HTTP 설정 ──────────────────────────────────────────────────────────────────
REQUEST_TIMEOUT_SECONDS  = 30
MAX_RETRY_ATTEMPTS       = 3
RETRY_WAIT_MIN_SECONDS   = 4
RETRY_WAIT_MAX_SECONDS   = 30

# ── OCI 설정 ───────────────────────────────────────────────────────────────────
from collectors.scripts.shared.config import OCI_NAMESPACE, OCI_BUCKET  # noqa

OCI_PREFIX     = "data/ai-info/"   # OCI 버킷 내 경로 prefix

# OCI 업로드 대상: (로컬 경로, OCI object name)
OCI_UPLOAD_TARGETS: list[tuple[Path, str]] = [
    (AI_INFO_DIR  / "integrated_major_models.json",  f"{OCI_PREFIX}integrated_major_models.json"),
    (RANKINGS_DIR / "model_benchmarks_records.json", f"{OCI_PREFIX}model_benchmarks_records.json"),
    (RANKINGS_DIR / "category_stats.json",           f"{OCI_PREFIX}category_stats.json"),
]
