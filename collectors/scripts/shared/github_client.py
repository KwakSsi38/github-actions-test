"""
shared/github_client.py — GitHub API 공통 클라이언트

prompts 파이프라인에서 사용하는 GitHub REST API 호출 로직.
토큰 로테이션, rate limit 자동 대기, 재시도 포함.
"""

import logging
import os
import time

import requests

logger = logging.getLogger(__name__)

GITHUB_TOKENS = [t for t in [
    os.environ.get("GITHUB_TOKEN_1"),
    os.environ.get("GITHUB_TOKEN_2"),
] if t]

BASE_DELAY = 0.3  # 5,000 req/hour 안전 딜레이


class GitHubTokenRotator:
    """GitHub API 토큰 로테이터. rate limit 소진 시 자동으로 다음 토큰으로 교체."""

    def __init__(self, tokens: list[str]):
        if not tokens:
            raise ValueError("GITHUB_TOKEN_1 환경변수가 설정되지 않았습니다.")
        self.tokens = tokens
        self.idx    = 0

    def headers(self, extra: dict | None = None) -> dict:
        h = {
            "Authorization":        f"token {self.tokens[self.idx]}",
            "Accept":               "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        }
        if extra:
            h.update(extra)
        return h

    def raw_headers(self) -> dict:
        return {"Authorization": f"token {self.tokens[self.idx]}"}

    def rotate(self) -> None:
        self.idx = (self.idx + 1) % len(self.tokens)
        logger.info("GitHub 토큰 %d번으로 교체", self.idx + 1)


# 모듈 수준 싱글턴 (enrich, contents 양쪽에서 공유)
rotator = GitHubTokenRotator(GITHUB_TOKENS)


def github_get(
    url: str,
    extra_headers: dict | None = None,
    retries: int = 3,
) -> tuple[int, dict | None, dict]:
    """
    GitHub API GET 요청.
    반환: (status_code, body, response_headers)
    rate limit / 429 자동 대기, 재시도 포함.
    """
    for attempt in range(retries):
        try: # TODO: 중복 로직 리펙토링 필요
            resp      = requests.get(url, headers=rotator.headers(extra_headers), timeout=30)
            remaining = int(resp.headers.get("X-RateLimit-Remaining", 9999))
            reset_at  = int(resp.headers.get("X-RateLimit-Reset", 0))

            if resp.status_code == 403 and remaining == 0:
                wait = max(reset_at - int(time.time()), 0) + 5
                logger.warning("Rate limit 소진 — %d초 대기", wait)
                time.sleep(wait)
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

            body = resp.json() if resp.status_code not in (304, 404, 403) else None # TODO: 상태 코드 200? 오류 날 가능성은 없는지 확인
            return resp.status_code, body, dict(resp.headers)

        except requests.exceptions.RequestException as e:
            logger.error("요청 오류 (시도 %d): %s", attempt + 1, e)
            time.sleep(5)

    return -1, None, {}