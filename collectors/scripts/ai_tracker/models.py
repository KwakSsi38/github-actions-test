"""
ai_tracker/models.py — 수집 아이템 데이터 생성 유틸

수집 아이템 딕셔너리 생성 및 ID/날짜 처리 함수 모음.
"""

import hashlib
import logging
from datetime import datetime, timezone
from typing import Any, Optional

from dateutil import parser as dateparser

log: logging.Logger = logging.getLogger(__name__)


def make_id(provider: str, title: str, url: str) -> str:
    """provider, title, url 기반 고유 ID 생성."""
    # TODO: title, url이 바뀌면 다른 걸로 취급될 것 보완 필요 TM-135
    raw: str = f"{provider}::{title}::{url}"
    return hashlib.sha256(raw.encode()).hexdigest()[:16]


def parse_date(raw: Optional[str]) -> str:
    """날짜 문자열을 UTC ISO 8601로 변환. 파싱 실패 시 현재 시각 반환."""
    if not raw:
        return datetime.now(timezone.utc).isoformat()
    try:
        return dateparser.parse(raw).astimezone(timezone.utc).isoformat()
    except Exception as e:
        log.debug("날짜 파싱 실패 (raw=%s): %s", raw, e)
        return datetime.now(timezone.utc).isoformat()


def make_item(
    source: dict[str, Any],
    title: str,
    url: str,
    summary: str,
    stype: str,
    pub_at: Optional[str] = None,
) -> dict[str, Any]:
    """수집 아이템 딕셔너리 생성."""
    return {
        "id":           make_id(source["provider"], title, url),
        "provider":     source["provider"],
        "source_type":  stype,
        "label":        source["label"],
        "title":        title,
        "url":          url,
        "summary":      summary,
        "raw_content":  "",
        "published_at": parse_date(pub_at),
    }