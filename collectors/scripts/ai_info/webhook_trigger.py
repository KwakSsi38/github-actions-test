"""
webhook_trigger.py — Spring 서버에 AI 데이터 파이프라인 실행 요청

raw 데이터가 OCI에 업로드된 후 Spring 서버의 Webhook 엔드포인트를 호출.
Spring에서 OCI raw 데이터를 읽어 가공 → 재업로드.

환경변수:
    SPRING_WEBHOOK_URL:    Spring 서버 Webhook 엔드포인트
    SPRING_WEBHOOK_SECRET: 인증 시크릿
"""

import logging
import os
import sys

import httpx

from collectors.scripts.shared.utils import setup_logging

logger = logging.getLogger(__name__)

TIMEOUT = 10


def main() -> None:
    setup_logging()

    webhook_url    = os.environ.get("SPRING_WEBHOOK_URL", "")
    webhook_secret = os.environ.get("SPRING_WEBHOOK_SECRET", "")

    if not webhook_url:
        logger.error("SPRING_WEBHOOK_URL 환경변수가 설정되지 않았습니다.")
        sys.exit(1)

    if not webhook_secret:
        logger.error("SPRING_WEBHOOK_SECRET 환경변수가 설정되지 않았습니다.")
        sys.exit(1)

    logger.info("Webhook 호출: %s", webhook_url)

    try:
        with httpx.Client(timeout=TIMEOUT) as client:
            resp = client.post(
                webhook_url,
                headers={"X-Webhook-Secret": webhook_secret},
                json={"event": "raw_data_updated"},
            )

        if resp.status_code == 200:
            logger.info("Webhook 성공: %s", resp.text)
        else:
            logger.error("Webhook 실패: HTTP %d — %s", resp.status_code, resp.text)
            sys.exit(1)

    except httpx.RequestError as e:
        logger.error("Webhook 요청 오류: %s", e)
        sys.exit(1)


if __name__ == "__main__":
    main()