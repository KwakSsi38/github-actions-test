"""
ai_tracker/notify.py — AI Tracker 파이프라인 Discord 알림

shared/notify.py의 send_embed / send_error를 사용합니다.
파이프라인 전용 알림 함수(notify_success, notify_failure)만 여기서 정의합니다.
"""

import logging

from collectors.scripts.shared.notify import send_embed, send_error
from collectors.scripts.shared.utils import now_display

log: logging.Logger = logging.getLogger(__name__)


def notify_success(count: int) -> None:
    """수집 및 OCI 업로드 성공 알림."""
    send_embed({
        "embeds": [{
            "title":       "✅ AI Tracker 수집 완료",
            "color":       0x57F287,
            "description": f"총 **{count}건** 수집 후 OCI 업로드 완료.",
            "footer":      {"text": now_display()},
        }]
    })


def notify_failure(step: str, exc: Exception) -> None:
    """파이프라인 단계별 실패 알림."""
    send_error(f"AI Tracker 파이프라인 실패 — {step}", exc)
