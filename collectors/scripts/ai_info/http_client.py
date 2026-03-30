"""
http_client.py — tenacity 기반 재시도 HTTP 클라이언트

5xx / 네트워크 오류는 지수 백오프로 최대 N회 재시도.
4xx 클라이언트 오류는 재시도 없이 즉시 종료.
"""

import logging
import sys

import httpx
from tenacity import (
    RetryError,
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
    before_sleep_log,
)

from collectors.scripts.shared.config import (
    MAX_RETRY_ATTEMPTS,
    REQUEST_TIMEOUT_SECONDS,
    RETRY_WAIT_MAX_SECONDS,
    RETRY_WAIT_MIN_SECONDS,
)

logger = logging.getLogger(__name__)


class _ClientError(Exception):
    """4xx 클라이언트 오류 — 재시도 없이 즉시 실패."""


# 재시도 대상: 네트워크 오류 + 5xx 서버 오류만
_RETRYABLE_EXCEPTIONS = (
    httpx.RequestError,     # 연결 실패, DNS 오류, 타임아웃 등
    httpx.HTTPStatusError,  # 5xx 서버 오류
)


@retry(
    retry=retry_if_exception_type(_RETRYABLE_EXCEPTIONS),
    stop=stop_after_attempt(MAX_RETRY_ATTEMPTS),
    wait=wait_exponential(
        multiplier=1,
        min=RETRY_WAIT_MIN_SECONDS,
        max=RETRY_WAIT_MAX_SECONDS,
    ),
    before_sleep=before_sleep_log(logger, logging.WARNING),
    reraise=True,
)
def _get_with_retry(url: str, headers: dict | None = None) -> dict:
    """단일 GET 요청. 5xx / 네트워크 오류 발생 시 tenacity가 자동 재시도."""
    with httpx.Client(timeout=REQUEST_TIMEOUT_SECONDS) as client:
        response = client.get(url, headers=headers or {})

        # 4xx: _ClientError로 변환 → retry 데코레이터 대상 제외, 즉시 상위 전파
        if 400 <= response.status_code < 500:
            raise _ClientError(f"클라이언트 오류 {response.status_code}: {url}")

        response.raise_for_status()  # 5xx → HTTPStatusError → retry 대상
        return response.json()


def fetch_json(url: str, headers: dict | None = None) -> dict:
    """
    지수 백오프 재시도 GET 요청.
    최종 실패 시 sys.exit(1)로 Actions에 실패 전파.
    """
    try:
        return _get_with_retry(url, headers)
    except _ClientError as e:
        logger.error("클라이언트 오류 (재시도 없음): %s", e)
        sys.exit(1)
    except RetryError as e:
        logger.error("최대 재시도 횟수(%d회) 초과: %s / 원인: %s", MAX_RETRY_ATTEMPTS, url, e)
        sys.exit(1)
    except Exception as e:
        logger.error("요청 실패 (재시도 불가): %s / 원인: %s", url, e)
        sys.exit(1)