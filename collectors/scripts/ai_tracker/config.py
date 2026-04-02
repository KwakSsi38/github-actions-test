"""
ai_tracker/config.py — AI Tracker 파이프라인 설정

환경변수, 수집 제한, CF BR 설정 등 ai_tracker 전용 상수 관리.
shared/config.py와 별개로 이 파이프라인에서만 사용하는 값을 정의.
"""

import os
from pathlib import Path
from typing import Any

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent.parent.parent / ".env")

# ── 환경변수 ──────────────────────────────────────────────────────────────────

CF_ACCOUNT_ID: str = os.environ.get("CF_ACCOUNT_ID", "")
CF_API_TOKEN: str  = os.environ.get("CF_API_TOKEN", "")

CF_BR_ENDPOINT_TPL: str = (
    "https://api.cloudflare.com/client/v4/accounts"
    "/{account_id}/browser-rendering/content"
)

_USER_AGENT: str = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/122.0.0.0 Safari/537.36"
)

# ── 수집 제한 ─────────────────────────────────────────────────────────────────

# 소스별 최대 수집 항목 수 (테스트 시 3, 운영 시 10 등으로 조정)
MAX_ITEMS_PER_SOURCE: int = 3
# 중복 제거 후 상세 본문 수집 대상 최대 항목 수
MAX_TOTAL_ITEMS: int = 15
# CF BR 호출 간 최소 대기 시간 (초) — 429 방지
CF_BR_BASE_DELAY_SECONDS: float = 8.0
# 상세 본문 수집 간 대기 시간 (초)
DETAIL_FETCH_DELAY_SECONDS: float = 3.0

# ── 파서 필터 ─────────────────────────────────────────────────────────────────

EXCLUDE_NAV_TEXTS: set[str] = {
    "Solutions", "Partners", "Learn", "Company", "About",
    "Help and security", "Terms and policies", "Resources", "Discord", "News",
}

# ── 수집 소스 ─────────────────────────────────────────────────────────────────

SOURCES: list[dict[str, Any]] = [
    {
        "provider": "OpenAI",
        "type": "rss",
        "url": "https://openai.com/news/rss.xml",
        "label": "OpenAI 뉴스",
    },
    {
        "provider": "OpenAI",
        "type": "scrape",
        "url": "https://developers.openai.com/api/docs/changelog/",
        "label": "OpenAI Platform Changelog",
        "needs_js": True,
        "parser": "openai_changelog",
    },
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
        "type": "scrape",
        # docs.anthropic.com은 platform.claude.com으로 리디렉션되며
        # Next.js SSR 페이지이므로 CF BR 필요
        "url": "https://docs.anthropic.com/en/release-notes/api",
        "label": "Anthropic API Changelog",
        "needs_js": True,
        "parser": "anthropic_changelog",
    },
    {
        "provider": "Google",
        "type": "rss",
        "url": "https://developers.googleblog.com/feeds/posts/default",
        "label": "Google Developers Blog",
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
