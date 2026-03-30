"""
prompts/notify.py — prompts 파이프라인 Discord 알림

shared/notify.py의 send_embed / send_error를 래핑해
prompts 파이프라인 전용 알림 함수를 제공.
"""

import logging

from collectors.scripts.prompts.config import FAIL_ALERT_THRESHOLD, SENSITIVE_VALUES
from collectors.scripts.shared.notify import send_embed, send_error
from collectors.scripts.shared.utils import now_display

logger = logging.getLogger(__name__)


def notify_start(new_count: int, existing_count: int, removed_count: int) -> None:
    send_embed({
        "embeds": [{
            "title":  "📢 SKILL.md 수집 시작",
            "color":  0x5865F2,
            "fields": [
                {"name": "신규",   "value": str(new_count),      "inline": True},
                {"name": "기존",   "value": str(existing_count), "inline": True},
                {"name": "사라진", "value": str(removed_count),  "inline": True},
            ],
            "footer": {"text": now_display()},
        }]
    })


def notify_complete(stats: dict, elapsed_sec: float, interrupted: bool = False) -> None:
    minutes = int(elapsed_sec // 60)
    seconds = int(elapsed_sec % 60)
    title   = "⏸️ 수집 중단 (다음 실행에서 재개)" if interrupted else "✅ 수집 완료"
    color   = 0xED4245 if interrupted else 0x57F287

    send_embed({
        "embeds": [{
            "title":  title,
            "color":  color,
            "fields": [
                {"name": "신규",   "value": str(stats.get("new", 0)),      "inline": True},
                {"name": "수정",   "value": str(stats.get("updated", 0)),  "inline": True},
                {"name": "스킵",   "value": str(stats.get("skipped", 0)),  "inline": True},
                {"name": "실패",   "value": str(stats.get("failed", 0)),   "inline": True},
                {"name": "사라진", "value": str(stats.get("removed", 0)),  "inline": True},
                {"name": "소요",   "value": f"{minutes}분 {seconds}초",    "inline": True},
            ],
            "footer": {"text": now_display()},
        }]
    })


def notify_fail_warning(fail_count: int, total: int) -> None:
    if fail_count < FAIL_ALERT_THRESHOLD:
        return
    send_embed({
        "embeds": [{
            "title":       "⚠️ 실패 임계치 초과",
            "color":       0xFEE75C,
            "description": f"처리 중 실패가 **{fail_count}개** 발생했습니다. (전체 {total}개)",
            "footer":      {"text": now_display()},
        }]
    })


def notify_error(exc: Exception) -> None:
    send_error("수집 오류 — 비정상 종료", exc, SENSITIVE_VALUES)
