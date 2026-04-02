"""
ai_tracker/collector.py — AI 모델 업데이트 수집기

수집 대상: OpenAI, Anthropic, Google
수집 방법:
  - RSS / Atom 피드
  - GitHub Releases API
  - 웹 스크래핑: httpx 우선 → 실패 시 Cloudflare Browser Rendering REST API

CLAUDE.md 준수 사항:
  - 모든 함수에 타입 힌트
  - logging 모듈 사용 (print 금지)
  - 복구 불가 오류 시 sys.exit(1)
  - 환경변수 시작 시 검증
"""

import hashlib
import logging
import os
import sys
import time
from datetime import datetime, timezone
from typing import Any, Optional

import feedparser
import httpx
from bs4 import BeautifulSoup, Tag
from dateutil import parser as dateparser
from tenacity import (
    RetryError,
    before_sleep_log,
    retry,
    retry_if_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from collectors.scripts.shared.utils import save_json_str, setup_logging, save_json

setup_logging()
log: logging.Logger = logging.getLogger(__name__)

# ── 환경변수 ──────────────────────────────────────────────────────────────────

def _optional_env(key: str) -> str:
    return os.getenv(key, "")


GITHUB_TOKEN: str  = _optional_env("GITHUB_TOKEN")
CF_ACCOUNT_ID: str = _optional_env("CF_ACCOUNT_ID")
CF_API_TOKEN: str  = _optional_env("CF_API_TOKEN")

CF_BR_ENDPOINT_TPL: str = (
    "https://api.cloudflare.com/client/v4/accounts"
    "/{account_id}/browser-rendering/content"
)

# ── 수집 소스 ─────────────────────────────────────────────────────────────────
# needs_js=True  → CF BR 직접 사용
# needs_js=False → httpx 우선, 항목 없으면 CF BR 폴백

SOURCES: list[dict[str, Any]] = [
    # OpenAI
    {
        "provider": "OpenAI",
        "type": "rss",
        "url": "https://openai.com/news/rss.xml",
        "label": "OpenAI 뉴스",
    },
    {
        "provider": "OpenAI",
        "type": "github_releases",
        "repo": "openai/openai-python",
        "label": "openai-python SDK",
    },
    {
        "provider": "OpenAI",
        "type": "scrape",
        "url": "https://developers.openai.com/api/docs/changelog/",
        "label": "OpenAI Platform Changelog",
        "needs_js": True,
        "parser": "openai_changelog",
    },
    # Anthropic
    {
        "provider": "Anthropic",
        "type": "scrape",
        "url": "https://www.anthropic.com/news",
        "label": "Anthropic 뉴스",
        "needs_js": False,
        "parser": "anthropic_news",
    },
    {
        "provider": "Anthropic",
        "type": "github_releases",
        "repo": "anthropics/anthropic-sdk-python",
        "label": "anthropic-sdk-python",
    },
    {
        "provider": "Anthropic",
        "type": "scrape",
        "url": "https://docs.anthropic.com/en/release-notes/api",
        "label": "Anthropic API Changelog",
        "needs_js": False,
        "parser": "anthropic_changelog",
    },
    # Google
    {
        "provider": "Google",
        "type": "rss",
        "url": "https://developers.googleblog.com/feeds/posts/default",
        "label": "Google Developers Blog",
    },
    {
        "provider": "Google",
        "type": "github_releases",
        "repo": "google-gemini/generative-ai-python",
        "label": "Gemini Python SDK",
    },
    {
        "provider": "Google",
        "type": "scrape",
        "url": "https://ai.google.dev/gemini-api/docs/changelog",
        "label": "Gemini API Changelog",
        "needs_js": True,
        "parser": "google_changelog",
    },
]

# ── 재시도 설정 ───────────────────────────────────────────────────────────────

_RETRY_KWARGS: dict[str, Any] = {
    "retry": retry_if_exception_type((httpx.TransportError, httpx.TimeoutException)),
    "stop": stop_after_attempt(3),
    "wait": wait_exponential(multiplier=1, min=2, max=10),
    "before_sleep": before_sleep_log(log, logging.WARNING),
    "reraise": True,
}

# ── 유틸 함수 ─────────────────────────────────────────────────────────────────

def make_id(provider: str, title: str, url: str) -> str:
    """provider, title, url 을 결합해 SHA-256 기반 16자 고유 ID를 생성합니다."""
    raw: str = f"{provider}::{title}::{url}"
    return hashlib.sha256(raw.encode()).hexdigest()[:16]


def parse_date(raw: Optional[str]) -> str:
    """다양한 날짜 포맷을 ISO 8601 UTC 문자열로 변환합니다."""
    if not raw:
        return datetime.now(timezone.utc).isoformat()
    try:
        return dateparser.parse(raw).astimezone(timezone.utc).isoformat()
    except Exception:
        return datetime.now(timezone.utc).isoformat()


def _make_item(
        source: dict[str, Any],
        title: str,
        url: str,
        summary: str,
        source_type: str,
        published_at: Optional[str] = None,
        tag: Optional[str] = None,
) -> dict[str, Any]:
    """수집 항목 딕셔너리를 일관된 형식으로 생성합니다."""
    return {
        "id":           make_id(source["provider"], title, url),
        "provider":     source["provider"],
        "source_type":  source_type,
        "label":        source["label"],
        "title":        title,
        "url":          url,
        "summary":      summary,
        "tag":          tag,
        "published_at": parse_date(published_at),
    }

# ── RSS 수집 ──────────────────────────────────────────────────────────────────

def collect_rss(source: dict[str, Any]) -> list[dict[str, Any]]:
    """
    RSS/Atom 피드에서 항목을 수집합니다.

    키워드 필터를 적용하지 않는 이유:
    이미 해당 회사의 공식 피드를 구독하므로 모든 항목이 관련 있습니다.
    과도한 필터링은 유용한 항목을 누락시킬 수 있습니다.
    최근 20개만 가져와 양을 제한합니다.
    """
    items: list[dict[str, Any]] = []
    try:
        feed = feedparser.parse(source["url"])
        for entry in feed.entries[:20]:
            title: str   = entry.get("title", "").strip()
            link: str    = entry.get("link", "")
            summary: str = entry.get("summary", "")
            if not title or not link:
                continue
            clean_summary: str = (
                BeautifulSoup(summary, "html.parser").get_text()[:500]
                if summary and "<" in summary
                else summary[:500] if summary else ""
            )
            items.append(_make_item(
                source=source,
                title=title,
                url=link,
                summary=clean_summary,
                source_type="rss",
                published_at=entry.get("published") or entry.get("updated"),
            ))
        log.info("[RSS] %s → %d건", source["label"], len(items))
    except Exception as exc:
        log.warning("[RSS] %s 실패: %s", source["label"], exc)
    return items

# ── GitHub Releases 수집 ──────────────────────────────────────────────────────

@retry(**_RETRY_KWARGS)
def _fetch_github_releases(url: str, headers: dict[str, str]) -> list[dict[str, Any]]:
    """GitHub Releases API를 호출합니다. 실패 시 tenacity가 재시도합니다."""
    with httpx.Client(timeout=15, follow_redirects=True) as client:
        resp = client.get(url, headers=headers)
        resp.raise_for_status()
        return resp.json()


def collect_github_releases(source: dict[str, Any]) -> list[dict[str, Any]]:
    """
    GitHub Releases API에서 정식 릴리즈를 수집합니다.

    draft/prerelease를 제외하는 이유:
    draft는 미완성 릴리즈, prerelease는 베타/RC 버전으로
    실제 사용자에게 영향을 주는 정식 릴리즈만 수집합니다.
    """
    headers: dict[str, str] = {"Accept": "application/vnd.github+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"

    api_url: str = (
        f"https://api.github.com/repos/{source['repo']}/releases?per_page=10"
    )
    try:
        releases: list[dict[str, Any]] = _fetch_github_releases(api_url, headers)
    except RetryError as exc:
        log.error("[GitHub] %s → 3회 재시도 모두 실패: %s", source["label"], exc)
        return []
    except Exception as exc:
        log.error("[GitHub] %s 실패: %s", source["label"], exc)
        return []

    items: list[dict[str, Any]] = []
    for rel in releases:
        if rel.get("draft") or rel.get("prerelease"):
            continue
        title: str = rel.get("name") or rel.get("tag_name", "")
        if not title:
            continue
        items.append(_make_item(
            source=source,
            title=f"{source['label']} {title}",
            url=rel.get("html_url", ""),
            summary=rel.get("body", "")[:500],
            source_type="github_release",
            published_at=rel.get("published_at"),
            tag=rel.get("tag_name"),
        ))
    log.info("[GitHub] %s → %d건", source["label"], len(items))
    return items

# ── 스크래핑: 사이트별 파서 ───────────────────────────────────────────────────
#
# 범용 파서(h2/h3만 보는 방식)를 쓰지 않는 이유:
# 3개 사이트의 HTML 구조가 각각 다릅니다.
# - OpenAI: 날짜 헤딩 + 그 아래 변경 항목 목록
# - Anthropic: 버전/날짜 헤딩 + 설명 단락
# - Google: 날짜 섹션 + 기능별 항목
# 하나의 범용 파서로는 엉뚱한 텍스트를 title로 잡거나
# summary가 비어있는 경우가 많아 품질이 낮아집니다.
# 사이트가 HTML 구조를 바꾸면 해당 파서만 수정하면 됩니다.

def _parse_openai_changelog(
        html: str, source: dict[str, Any]
) -> list[dict[str, Any]]:
    """
    OpenAI API Changelog 파서 (https://developers.openai.com/api/docs/changelog/).

    CF BR 응답은 {"success":true,"result":"...html..."} JSON 래퍼로 오므로
    result 필드에서 HTML을 추출합니다.

    구조: h3(월) → 형제 div들 안에 Badge(날짜) + MarkdownContent(내용)
    """
    import json as _json

    # CF BR JSON 래퍼 처리
    raw = html.strip()
    if raw.startswith("{"):
        try:
            data = _json.loads(raw)
            html = data.get("result", html)
        except Exception:
            pass

    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    # _MarkdownContent_ 클래스를 포함한 div를 모두 찾아 역순으로 접근
    content_divs = [
        div for div in soup.find_all("div")
        if any("MarkdownContent" in cls for cls in div.get("class", []))
    ]

    for content_div in content_divs[:20]:
        # 같은 grid 행에서 날짜 배지 찾기 (형제 또는 부모의 자식)
        parent = content_div.parent
        date_text: str = ""
        if parent:
            badge = next(
                (
                    d for d in parent.find_all("div")
                    if any("Badge" in cls for cls in d.get("class", []))
                ),
                None,
            )
            if badge:
                date_text = badge.get_text(strip=True)

        para = content_div.find("p")
        title: str = para.get_text(strip=True)[:120] if para else ""
        summary: str = content_div.get_text(strip=True)[:500]

        if not title:
            continue

        items.append(_make_item(
            source=source,
            title=title,
            url=source["url"],
            summary=summary,
            source_type="scrape",
            published_at=date_text or None,
        ))

    return items


def _parse_anthropic_changelog(
        html: str, source: dict[str, Any]
) -> list[dict[str, Any]]:
    """
    Anthropic API Release Notes 파서.
    버전/날짜 헤딩(h2/h3) + 아래 설명 단락을 수집합니다.
    """
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    for heading in soup.select("h2, h3")[:20]:
        title: str = heading.get_text(strip=True)
        if not title:
            continue

        summary_parts: list[str] = []
        sibling = heading.find_next_sibling()
        while sibling and sibling.name not in ("h2", "h3"):
            if isinstance(sibling, Tag) and sibling.name == "p":
                text: str = sibling.get_text(strip=True)
                if text:
                    summary_parts.append(text)
            sibling = sibling.find_next_sibling()

        summary: str = " ".join(summary_parts)[:400]
        items.append(_make_item(
            source=source,
            title=title,
            url=source["url"],
            summary=summary,
            source_type="scrape",
        ))

    return items


def _parse_google_changelog(
        html: str, source: dict[str, Any]
) -> list[dict[str, Any]]:
    """
    Google Gemini API Changelog 파서.
    날짜 헤딩(h2) + 아래 변경 항목(ul > li) 구조를 수집합니다.
    """
    # CF BR JSON 래퍼 처리
    if html.strip().startswith("{"):
        try:
            import json as _json
            data = _json.loads(html)
            html = data.get("result", html)
        except Exception:
            pass

    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []

    for section in soup.select("h2")[:20]:
        date_text: str = section.get_text(strip=True)
        if not any(char.isdigit() for char in date_text):
            continue

        # h2 바로 다음 ul의 li 항목들을 수집
        sibling = section.find_next_sibling()
        while sibling and sibling.name not in ("h2",):
            if isinstance(sibling, Tag) and sibling.name == "ul":
                for li in sibling.find_all("li", recursive=False):
                    title: str = li.get_text(strip=True)[:120]
                    if title:
                        items.append(_make_item(
                            source=source,
                            title=f"{date_text}: {title}",
                            url=source["url"],
                            summary=li.get_text(strip=True)[:500],
                            source_type="scrape",
                            published_at=date_text,
                        ))
            sibling = sibling.find_next_sibling()

        if len(items) >= 20:
            break

    return items


def _parse_anthropic_news(
        html: str, source: dict[str, Any]
) -> list[dict[str, Any]]:
    """
    Anthropic 뉴스룸 HTML 구조에 최적화된 파서.
    상단 Featured 영역과 하단 News List 영역을 모두 지원합니다.
    """
    soup = BeautifulSoup(html, "html.parser")
    items: list[dict[str, Any]] = []
    base_url: str = "https://www.anthropic.com"
    seen_urls: set[str] = set()

    # 1. 하단 뉴스 리스트 (Publication List) 처리
    # 클래스명에 'PublicationList'와 'list'가 포함된 ul 내의 li들을 탐색
    list_items = soup.select('ul[class*="PublicationList"] li')

    # 2. 상단 피쳐드 그리드 (Featured Grid) 처리
    # 클래스명에 'FeaturedGrid'가 포함된 영역 내의 링크들
    featured_links = soup.select('div[class*="FeaturedGrid"] a[href*="/news/"]')

    # 두 그룹 통합 처리
    candidates = [('list', li) for li in list_items] + [('featured', a) for a in featured_links]

    for type_, entry in candidates:
        # a 태그 찾기
        anchor = entry if entry.name == "a" else entry.find("a", href=True)
        if not anchor:
            continue

        href: str = anchor["href"]
        # 메일 주소나 단순 경로 제외, 구체적인 뉴스 슬러그가 있는 것만 수집
        if not (href.startswith("/news/") and len(href) > 6):
            continue

        url: str = href if href.startswith("http") else f"{base_url}{href}"
        if url in seen_urls:
            continue
        seen_urls.add(url)

        # 타이틀 및 날짜 추출 (영역별 구조 차이 반영)
        title: str = ""
        date_text: str = ""
        summary: str = ""

        if type_ == 'list':
            # 하단 리스트 구조: 별도의 span 클래스에 제목이 담겨 있음
            title_tag = anchor.select_one('span[class*="title"]')
            title = title_tag.get_text(strip=True) if title_tag else ""

            time_tag = anchor.find("time")
            date_text = time_tag.get_text(strip=True) if time_tag else ""
        else:
            # 상단 그리드 구조: h2 또는 h4 태그가 제목
            title_tag = anchor.find(["h2", "h4"])
            title = title_tag.get_text(strip=True) if title_tag else ""

            time_tag = anchor.find("time")
            date_text = time_tag.get_text(strip=True) if time_tag else ""

            # 피쳐드 영역은 p 태그에 요약문이 있음
            summary_tag = anchor.find("p")
            summary = summary_tag.get_text(strip=True) if summary_tag else ""

        # 제목이 비어있으면 앵커 텍스트에서 날짜/카테고리를 제거하고 추출 시도
        if not title:
            title = anchor.get_text(" ", strip=True)
            if date_text:
                title = title.replace(date_text, "").strip()
            # 카테고리(Announcements, Policy 등)가 붙어있을 경우 제거
            for cat in ["Announcements", "Policy", "Research", "Product", "Event"]:
                if title.startswith(cat):
                    title = title[len(cat):].strip()

        if not title:
            continue

        items.append(_make_item(
            source=source,
            title=title,
            url=url,
            summary=summary,
            source_type="scrape",
            published_at=date_text or None,
        ))

        if len(items) >= 30: # 넉넉하게 수집
            break

    return items


_PARSERS: dict[str, Any] = {
    "openai_changelog":    _parse_openai_changelog,
    "anthropic_news":      _parse_anthropic_news,
    "anthropic_changelog": _parse_anthropic_changelog,
    "google_changelog":    _parse_google_changelog,
}


def _parse_html(html: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    """source에 지정된 파서를 선택해 HTML을 파싱합니다."""
    parser_key: str = source.get("parser", "")
    parser_fn = _PARSERS.get(parser_key)
    if not parser_fn:
        log.warning("[Scrape] 알 수 없는 파서: %s — 빈 목록 반환", parser_key)
        return []
    return parser_fn(html, source)

# ── 스크래핑: HTTP 요청 ───────────────────────────────────────────────────────

@retry(**_RETRY_KWARGS)
def _fetch_html_with_httpx(url: str) -> str:
    """httpx로 HTML을 가져옵니다. 실패 시 tenacity가 재시도합니다."""
    with httpx.Client(timeout=15, follow_redirects=True) as client:
        resp = client.get(
            url,
            headers={"User-Agent": "Mozilla/5.0 (AI-Update-Tracker/1.0)"},
        )
        resp.raise_for_status()
        return resp.text


def _scrape_with_httpx(
        source: dict[str, Any],
) -> Optional[list[dict[str, Any]]]:
    """
    1차 스크래핑: httpx + tenacity 재시도.

    None 반환 시 CF BR 폴백으로 넘어갑니다.
    빈 리스트([])는 요청은 성공했으나 파싱 결과가 없는 경우로,
    CF BR 폴백을 시도합니다 (JS 렌더링 후 다시 파싱).
    """
    try:
        html: str = _fetch_html_with_httpx(source["url"])
        items: list[dict[str, Any]] = _parse_html(html, source)
        if items:
            log.info("[Scrape/HTTP] %s → %d건", source["label"], len(items))
            return items
        log.info("[Scrape/HTTP] %s → 항목 없음, CF BR 폴백", source["label"])
        return None
    except RetryError:
        log.warning("[Scrape/HTTP] %s → 3회 재시도 실패, CF BR 폴백", source["label"])
        return None
    except Exception as exc:
        log.warning("[Scrape/HTTP] %s 실패: %s — CF BR 폴백", source["label"], exc)
        return None


def _scrape_with_cf_br(source: dict[str, Any]) -> list[dict[str, Any]]:
    """
    2차 폴백: Cloudflare Browser Rendering REST API (/content).

    httpx로 빈 결과가 나오는 경우 JS로 동적 렌더링되는 페이지일 수 있습니다.
    CF BR은 실제 브라우저(Chromium)로 페이지를 렌더링한 뒤 HTML을 반환하므로
    JS 렌더링 이후의 콘텐츠까지 수집할 수 있습니다.

    Free 플랜: 하루 10분 / 동시 3개 브라우저.
    이미지/폰트/미디어를 차단해 불필요한 브라우저 시간 낭비를 줄입니다.
    """
    if not CF_ACCOUNT_ID or not CF_API_TOKEN:
        log.error(
            "[Scrape/CF] CF_ACCOUNT_ID / CF_API_TOKEN 미설정 — %s 수집 불가",
            source["label"],
        )
        return []

    endpoint: str = CF_BR_ENDPOINT_TPL.format(account_id=CF_ACCOUNT_ID)
    try:
        # Free 플랜 rate limit 대비: 연속 CF BR 요청 간 간격 확보
        time.sleep(3)
        with httpx.Client(timeout=40) as client:
            resp = client.post(
                endpoint,
                headers={
                    "Authorization": f"Bearer {CF_API_TOKEN}",
                    "Content-Type": "application/json",
                },
                json={
                    "url": source["url"],
                    "rejectResourceTypes": ["image", "font", "media", "stylesheet"],
                    "gotoOptions": {
                        "waitUntil": "networkidle2",
                        "timeout":   25000,
                    },
                },
            )
            resp.raise_for_status()
        items: list[dict[str, Any]] = _parse_html(resp.text, source)
        log.info("[Scrape/CF] %s → %d건", source["label"], len(items))
        return items
    except Exception as exc:
        log.error("[Scrape/CF] %s 실패: %s", source["label"], exc)
        return []


def collect_scrape(source: dict[str, Any]) -> list[dict[str, Any]]:
    """스크래핑 전략: httpx 우선 → 실패/빈 결과 시 CF BR 폴백."""
    if source.get("needs_js", False):
        return _scrape_with_cf_br(source)
    result: Optional[list[dict[str, Any]]] = _scrape_with_httpx(source)
    return result if result is not None else _scrape_with_cf_br(source)

# ── 메인 ─────────────────────────────────────────────────────────────────────

def collect_all() -> list[dict[str, Any]]:
    """모든 소스에서 항목을 수집하고 중복을 제거한 목록을 반환합니다."""
    all_items: list[dict[str, Any]] = []
    failed: list[str] = []

    for source in SOURCES:
        try:
            match source["type"]:
                case "rss":
                    all_items.extend(collect_rss(source))
                case "github_releases":
                    all_items.extend(collect_github_releases(source))
                case "scrape":
                    all_items.extend(collect_scrape(source))
                case _:
                    log.warning("알 수 없는 수집 타입: %s", source["type"])
        except Exception as exc:
            msg: str = f"{source['label']}: {exc}"
            failed.append(msg)
            log.error("[수집 오류] %s", msg)

    # id 기준 중복 제거 (동일 내용이 여러 소스에서 수집될 경우 대비)
    seen: set[str] = set()
    unique: list[dict[str, Any]] = [
        item for item in all_items
        if item["id"] not in seen and not seen.add(item["id"])  # type: ignore[func-returns-value]
    ]
    log.info("총 %d건 수집 (중복 제거 후), 실패 소스: %d개", len(unique), len(failed))

    if failed:
        log.warning("수집 실패 소스 %d개: %s", len(failed), failed)

    return unique


def build_payload(items: list[dict[str, Any]]) -> dict[str, Any]:
    """수집 결과를 OCI 업로드용 payload로 조립합니다."""
    return {
        "collected_at": datetime.now(timezone.utc).isoformat(),
        "count":        len(items),
        "items":        items,
    }


def build_json(items: list[dict[str, Any]]) -> str:
    """수집 결과를 JSON 문자열로 직렬화합니다. shared.utils.save_json_str 위임."""
    return save_json_str(build_payload(items))


if __name__ == "__main__":
    collected: list[dict[str, Any]] = collect_all()
    if not collected:
        log.error("수집된 항목이 없습니다 — 워크플로우를 실패로 처리합니다.")
        sys.exit(1)
    log.info("수집 완료: %d건 → oci_manager에서 OCI 업로드 진행", len(collected))