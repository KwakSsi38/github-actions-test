"""
ai_tracker/parsers.py — 소스별 HTML 파서

각 수집 소스의 HTML 구조에 맞는 파서 함수 모음.
새 소스 추가 시 파서 함수를 작성하고 PARSERS 딕셔너리에 등록.
"""

import json
import logging
from typing import Any

from bs4 import BeautifulSoup

from collectors.scripts.ai_tracker.config import EXCLUDE_NAV_TEXTS, MAX_ITEMS_PER_SOURCE
from collectors.scripts.ai_tracker.models import make_item

log: logging.Logger = logging.getLogger(__name__)


def parse_anthropic_news(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """Anthropic 뉴스 페이지 파싱. 상위 MAX_ITEMS_PER_SOURCE개 항목 반환."""
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []
    main_area = soup.find("main") or soup
    for li in main_area.select("li")[: MAX_ITEMS_PER_SOURCE * 5]:
        anchor = li.find("a", href=True)
        if not anchor:
            continue
        href: str = anchor["href"]
        if not href.startswith(("/news/", "/mars", "/research/")):
            continue
        title: str = anchor.get_text(strip=True)
        if title in EXCLUDE_NAV_TEXTS:
            continue
        url: str = (
            f"https://www.anthropic.com{href}" if href.startswith("/") else href
        )
        p_tag = li.find("p")
        summary: str = p_tag.get_text(strip=True) if p_tag else title
        items.append(make_item(source, title, url, summary, "scrape"))
        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break
    return items

# TODO: 현재 changelog 관련 가져오기가 잘 되지 않아 추후 수정 예정 TM-135
def parse_openai_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """OpenAI Platform Changelog 파싱. CF BR 응답이 JSON 래퍼일 경우 내부 HTML 추출."""
    if html.strip().startswith("{"):
        try:
            html = json.loads(html).get("result", html)
        except Exception as e:
            log.warning("[openai_changelog] JSON 언래핑 실패: %s", e)

    # TODO: 구조가 바뀐 경우를 대비해 범용 백업 로직 필요 TM-135
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []
    for div in soup.find_all("div", class_=lambda c: c and "MarkdownContent" in c)[:MAX_ITEMS_PER_SOURCE]:
        para = div.find("p")
        if para:
            items.append(
                make_item(source, para.get_text(strip=True), source["url"], div.get_text(strip=True), "scrape")
            )
    return items

def parse_anthropic_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """
    Anthropic API Changelog 파싱.

    platform.claude.com/docs/en/release-notes/overview 페이지 구조:
      <h2>날짜 (예: "March 2025")</h2>
      <p> 또는 <ul> — 변경 내용

    네비게이션의 h2/h3("Solutions", "Partners" 등)는 EXCLUDE_NAV_TEXTS로 필터링.
    h3도 날짜 헤딩으로 사용될 수 있어 h2, h3 모두 탐색.
    """
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    for heading in soup.select("h2, h3"):
        if len(items) >= MAX_ITEMS_PER_SOURCE:
            break

        date_text: str = heading.get_text(strip=True)

        # 네비게이션 메뉴 오염 방어
        if date_text in EXCLUDE_NAV_TEXTS or not date_text:
            continue

        # 날짜 형식이 아닌 단독 단어 헤딩 스킵 (예: "API", "SDK")
        words = date_text.split()
        if len(words) == 1 and not any(ch.isdigit() for ch in date_text):
            continue

        # 헤딩 다음 형제 요소에서 변경 내용 수집
        content_parts: list[str] = []
        sib = heading.find_next_sibling()
        while sib and sib.name not in ("h2", "h3"):
            if sib.name == "ul":
                content_parts.extend(
                    li.get_text(strip=True)
                    for li in sib.find_all("li", recursive=False)
                )
            elif sib.name == "p":
                text = sib.get_text(strip=True)
                if text:
                    content_parts.append(text)
            sib = sib.find_next_sibling()

        if not content_parts:
            continue

        title: str   = f"{date_text}: {content_parts[0]}"
        summary: str = " | ".join(content_parts)
        items.append(make_item(source, title, source["url"], summary, "scrape", date_text))

    return items


def parse_google_changelog(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """Gemini API Changelog 파싱. CF BR 응답이 JSON 래퍼일 경우 내부 HTML 추출."""
    if html.strip().startswith("{"):
        try:
            html = json.loads(html).get("result", html)
        except Exception as e:
            log.warning("[google_changelog] JSON 언래핑 실패: %s", e)

    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []
    for h2 in soup.select("h2")[:MAX_ITEMS_PER_SOURCE]:
        date_text: str = h2.get_text(strip=True)
        sib = h2.find_next_sibling()
        if not sib or sib.name != "ul":
            continue
        for li in sib.find_all("li", recursive=False)[:5]:
            li_text: str = li.get_text(strip=True)
            items.append(
                make_item(source, f"{date_text}: {li_text}", source["url"], li_text, "scrape", date_text)
            )
    return items


# 파서 키 → 함수 매핑. 새 소스 추가 시 여기에 등록.
PARSERS: dict[str, Any] = {
    "openai_changelog":    parse_openai_changelog,
    "anthropic_news":      parse_anthropic_news,
    "anthropic_changelog": parse_anthropic_changelog,
    "google_changelog":    parse_google_changelog,
}