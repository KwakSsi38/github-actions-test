"""
ai_tracker/collector.py — AI 모델 업데이트 수집 엔진

collect_all()로 모든 소스에서 항목을 수집하고,
build_json()으로 OCI 업로드용 JSON 문자열을 반환합니다.

pipeline.py가 이 모듈을 호출하는 진입점입니다.
"""

import logging
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timezone
from typing import Any

import feedparser
from bs4 import BeautifulSoup

from collectors.scripts.ai_tracker.config import (
    DETAIL_FETCH_DELAY_SECONDS,
    MAX_ITEMS_PER_SOURCE,
    MAX_TOTAL_ITEMS,
    SOURCES,
)
from collectors.scripts.ai_tracker.fetcher import fetch_detail_content, fetch_scrape_html
from collectors.scripts.ai_tracker.models import make_item
from collectors.scripts.ai_tracker.parsers import PARSERS
from collectors.scripts.shared.utils import save_json_str

log: logging.Logger = logging.getLogger(__name__)


# ── 환경변수 검증 ─────────────────────────────────────────────────────────────

def validate_env() -> None:
    """필수 환경변수 존재 여부 검증. 누락 시 즉시 종료."""
    missing = [v for v in ("CF_ACCOUNT_ID", "CF_API_TOKEN") if not os.environ.get(v)]
    if missing:
        log.error("필수 환경변수 미설정: %s", ", ".join(missing))
        sys.exit(1)


# ── RSS 수집 ──────────────────────────────────────────────────────────────────

def collect_rss(source: dict[str, Any]) -> list[dict[str, Any]]:
    """RSS/Atom 피드에서 최신 MAX_ITEMS_PER_SOURCE개 항목 수집."""
    items: list[dict[str, Any]] = []
    try:
        feed = feedparser.parse(source["url"])
        for entry in feed.entries[:MAX_ITEMS_PER_SOURCE]:
            title: str = entry.get("title", "").strip()
            link: str  = entry.get("link", "")
            if not title or not link:
                continue
            summary: str = BeautifulSoup(
                entry.get("summary", ""), "html.parser"
            ).get_text()
            items.append(make_item(source, title, link, summary, "rss", entry.get("published")))
    except Exception as e:
        log.error("RSS 수집 실패 [%s]: %s", source["label"], e)
    return items


# ── 수집 엔진 ─────────────────────────────────────────────────────────────────

def collect_all() -> list[dict[str, Any]]:
    """
    모든 소스에서 항목 수집, 중복 제거 후 상세 본문 수집.

    수집 전략:
      - RSS 소스: ThreadPoolExecutor로 병렬 수집 (I/O 대기 시간 단축)
      - scrape 소스: CF BR rate limit 보호를 위해 순차 수집
    """
    rss_sources    = [s for s in SOURCES if s["type"] == "rss"]
    scrape_sources = [s for s in SOURCES if s["type"] == "scrape"]

    all_items: list[dict[str, Any]] = []

    # ── RSS 병렬 수집 (rate-limit 없음) ─────────────────────────────────────
    log.info("RSS 소스 %d개 병렬 수집 시작...", len(rss_sources))
    with ThreadPoolExecutor(max_workers=len(rss_sources)) as executor:
        future_to_source = {
            executor.submit(collect_rss, source): source
            for source in rss_sources
        }
        for future in as_completed(future_to_source):
            source = future_to_source[future]
            try:
                items = future.result()
                log.info("RSS 수집 완료 [%s]: %d건", source["label"], len(items))
                all_items.extend(items)
            except Exception as e:
                log.error("RSS 수집 실패 [%s]: %s", source["label"], e)

    # ── scrape 순차 수집 (CF BR rate limit 보호) ─────────────────────────────
    log.info("scrape 소스 %d개 순차 수집 시작...", len(scrape_sources))
    for source in scrape_sources:
        try:
            html = fetch_scrape_html(source)
            if not html:
                log.warning("HTML 수집 결과 없음 [%s] — 스킵", source["label"])
                continue
            parser_fn = PARSERS.get(source["parser"])
            if parser_fn is None:
                log.error("알 수 없는 파서 키: %s", source["parser"])
                continue
            items = parser_fn(html, source)
            log.info("scrape 수집 완료 [%s]: %d건", source["label"], len(items))
            all_items.extend(items)
        except Exception as e:
            log.error("수집 실패 [%s]: %s", source["label"], e)

    # ── 중복 제거 (삽입 순서 유지) ────────────────────────────────────────────
    seen: set[str] = set()
    unique_items: list[dict[str, Any]] = []
    for item in all_items:
        if item["id"] not in seen:
            seen.add(item["id"])
            unique_items.append(item)

    unique_items = unique_items[:MAX_TOTAL_ITEMS]
    # TODO: published_at으로 처리하는 로직 추가 (null인 경우가 있으니 이를 고려) Tm-135
    log.info("선별된 %d개 항목의 상세 본문 수집 시작...", len(unique_items))

    for item in unique_items:
        if "changelog" in item["label"].lower():
            # changelog는 파싱 시점에 이미 요약이 구성됨
            item["raw_content"] = item["summary"]
        elif len(item["summary"]) > 1000:
            # 요약이 충분히 길면 상세 수집 생략
            # TODO: 필드가 실질적으로 본문을 포함하는지 체크 필요 TM-135
            item["raw_content"] = item["summary"]
        else:
            time.sleep(DETAIL_FETCH_DELAY_SECONDS)
            detail = fetch_detail_content(item["url"], item["provider"])
            item["raw_content"] = detail if detail else item["summary"]

    return unique_items


def build_json(items: list[dict[str, Any]]) -> str:
    """수집 결과를 OCI 업로드용 JSON 문자열로 직렬화."""
    return save_json_str({
        "collected_at": datetime.now(timezone.utc).isoformat(),
        "count":        len(items),
        "items":        items,
    })