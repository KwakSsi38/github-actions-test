"""
ai_tracker/fetcher.py — HTTP 수집 클라이언트

CF BR(Cloudflare Browser Rendering), httpx를 사용한 페이지 수집 로직.
collector.py의 상수(CF_ACCOUNT_ID, CF_API_TOKEN 등)를 참조합니다.
"""

import logging
import time
from typing import Any

import httpx
from bs4 import BeautifulSoup

from collectors.scripts.ai_tracker.config import (
    CF_ACCOUNT_ID,
    CF_API_TOKEN,
    CF_BR_BASE_DELAY_SECONDS,
    CF_BR_ENDPOINT_TPL,
    _USER_AGENT,
)

log: logging.Logger = logging.getLogger(__name__)


def extract_text_from_soup(soup: BeautifulSoup) -> str:
    """불필요한 태그를 제거하고 본문 텍스트만 추출."""
    for tag in soup(["script", "style", "nav", "footer", "header", "aside"]):
        tag.decompose()
    content = (
        soup.find("article")
        or soup.find("main")
        or soup.find("div", id="main-content")
    )
    target = content if content else soup
    return target.get_text(separator="\n", strip=True)


def fetch_with_cf_br(url: str, retries: int = 3) -> str:
    """
    Cloudflare Browser Rendering API로 JS 렌더링 페이지 수집.
    429 응답 시 Retry-After 헤더를 읽어 대기 후 재시도.
    """
    if not CF_API_TOKEN:
        log.warning("[CF] CF_API_TOKEN 미설정 — CF BR 스킵: %s", url)
        return ""

    endpoint = CF_BR_ENDPOINT_TPL.format(account_id=CF_ACCOUNT_ID)

    for attempt in range(1, retries + 1):
        try:
            time.sleep(CF_BR_BASE_DELAY_SECONDS)
            with httpx.Client(timeout=40) as client:
                resp = client.post(
                    endpoint,
                    headers={"Authorization": f"Bearer {CF_API_TOKEN}"},
                    json={
                        "url": url,
                        "rejectResourceTypes": ["image", "font", "media"],
                        "gotoOptions": {"waitUntil": "networkidle2"},
                    },
                )

            if resp.status_code == 200:
                return extract_text_from_soup(BeautifulSoup(resp.text, "html.parser"))

            if resp.status_code == 429:
                wait = int(resp.headers.get("Retry-After", 10))
                log.warning(
                    "[CF] 429 Too Many Requests — %d초 대기 후 재시도 (시도 %d/%d): %s",
                    wait, attempt, retries, url,
                )
                time.sleep(wait)
                continue

            log.warning("[CF] HTTP %d 응답 (시도 %d/%d): %s", resp.status_code, attempt, retries, url)

        except Exception as e:
            log.warning("[CF] 요청 오류 (시도 %d/%d): %s — %s", attempt, retries, url, e)

    log.error("[CF] %d회 재시도 후 최종 실패: %s", retries, url)
    return ""


def fetch_scrape_html(source: dict[str, Any]) -> str:
    """
    scrape 소스의 HTML 수집.
    needs_js=False: httpx 우선, 실패 시 CF BR 폴백.
    needs_js=True:  CF BR 직접 사용.
    """
    if source.get("needs_js"):
        return fetch_with_cf_br(source["url"])

    try:
        with httpx.Client(timeout=15, follow_redirects=True) as client:
            resp = client.get(source["url"], headers={"User-Agent": _USER_AGENT})

        if resp.status_code == 200:
            return resp.text

        if resp.status_code in (401, 403):
            log.debug("httpx 접근 차단 (HTTP %d) — CF BR 폴백: %s", resp.status_code, source["url"])
            return fetch_with_cf_br(source["url"])

        log.warning("httpx HTTP %d — CF BR 폴백: %s", resp.status_code, source["url"])
        return fetch_with_cf_br(source["url"])

    except httpx.RequestError as e:
        log.warning("httpx 요청 오류 — CF BR 폴백: %s — %s", source["url"], e)
        return fetch_with_cf_br(source["url"])


def fetch_detail_content(url: str, provider: str) -> str:
    """
    상세 페이지 본문 수집.
    OpenAI openai.com/index 경로는 403 방지를 위해 CF BR로 직접 수집.
    그 외: httpx 시도 → 403/401 시 CF BR 폴백.
    """
    if provider == "OpenAI" and "openai.com/index" in url:
        return fetch_with_cf_br(url)

    try:
        with httpx.Client(timeout=15, follow_redirects=True) as client:
            resp = client.get(url, headers={"User-Agent": _USER_AGENT})

        if resp.status_code in (401, 403):
            log.debug("httpx 접근 차단 (HTTP %d) — CF BR 폴백: %s", resp.status_code, url)
            return fetch_with_cf_br(url)

        resp.raise_for_status()
        return extract_text_from_soup(BeautifulSoup(resp.text, "html.parser"))

    except httpx.HTTPStatusError as e:
        log.warning("httpx HTTP 오류 — CF BR 폴백: %s — %s", url, e)
        return fetch_with_cf_br(url)
    except httpx.RequestError as e:
        log.warning("httpx 요청 오류 — CF BR 폴백: %s — %s", url, e)
        return fetch_with_cf_br(url)
