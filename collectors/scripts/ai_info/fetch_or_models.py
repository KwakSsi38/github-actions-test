"""
fetch_or_models.py — OpenRouter 모델 원본 데이터 수집

OpenRouter API에서 전체 모델 목록을 가져와 raw JSON으로 저장.
다음 단계(filter_models)에서 이 파일을 읽어 필터링.
"""

import logging
import sys

from collectors.scripts.ai_info.config import AI_INFO_DIR, OPENROUTER_MODELS_URL
from collectors.scripts.ai_info.http_client import fetch_json
from collectors.scripts.ai_info.utils import save_json, setup_logging

logger = logging.getLogger(__name__)

OUTPUT_FILE = AI_INFO_DIR / "models_info_raw.json"


def main() -> None:
    setup_logging()
    logger.info("OpenRouter 모델 원본 데이터 수집 시작")

    data = fetch_json(OPENROUTER_MODELS_URL)

    model_count = len(data.get("data", []))
    if model_count == 0:
        logger.error("수집된 모델이 없습니다. API 응답을 확인하세요.")
        sys.exit(1)

    save_json(data, OUTPUT_FILE)
    logger.info("수집 완료: 총 %d개 모델 저장 → %s", model_count, OUTPUT_FILE)


if __name__ == "__main__":
    main()
