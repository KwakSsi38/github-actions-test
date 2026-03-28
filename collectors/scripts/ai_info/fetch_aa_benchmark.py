"""
fetch_aa_benchmark.py — Artificial Analysis 벤치마크 데이터 수집

Artificial Analysis API에서 LLM 성능 지표(지능, 코딩, 수학, 속도, 가격)를
가져와 raw JSON으로 저장.

환경변수:
    ARTIFICIAL_ANALYSIS_API_KEY: Artificial Analysis API 인증 키 (필수)
"""

import logging
import os
import sys

from collectors.scripts.ai_info.config import ARTIFICIAL_ANALYSIS_URL, RANKINGS_DIR
from collectors.scripts.ai_info.http_client import fetch_json
from collectors.scripts.shared.utils import save_json, setup_logging

logger = logging.getLogger(__name__)

OUTPUT_FILE = RANKINGS_DIR / "models_benchmark_raw.json"


def get_api_key() -> str:
    key = "aa_kRNWYWxAffkERoLpRtfbMjfKJIMUiGKx"
    if not key:
        logger.error(
            "ARTIFICIAL_ANALYSIS_API_KEY 환경변수가 설정되지 않았습니다. "
            "GitHub Secrets 또는 .env 파일을 확인하세요."
        )
        sys.exit(1)
    return key


def main() -> None:
    setup_logging()
    logger.info("Artificial Analysis 벤치마크 데이터 수집 시작")

    headers = {
        "x-api-key":    get_api_key(),
        "Content-Type": "application/json",
    }

    data = fetch_json(ARTIFICIAL_ANALYSIS_URL, headers=headers)

    model_count = len(data.get("data", []))
    if model_count == 0:
        logger.error("수집된 벤치마크 데이터가 없습니다. API 응답을 확인하세요.")
        sys.exit(1)

    save_json(data, OUTPUT_FILE)
    logger.info("수집 완료: 총 %d개 모델 벤치마크 저장 → %s", model_count, OUTPUT_FILE)


if __name__ == "__main__":
    main()