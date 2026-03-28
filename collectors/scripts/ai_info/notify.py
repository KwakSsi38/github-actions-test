"""
ai_info/notify.py — ai_info 파이프라인 Discord 알림

shared/notify.py의 send_embed / send_error를 래핑해
ai_info 파이프라인 전용 알림 함수를 제공.
"""

import logging
import os

from collectors.scripts.shared.notify import send_embed, send_error
from collectors.scripts.shared.utils import now_display

logger = logging.getLogger(__name__)

_SENSITIVE_VALUES = [
    v for v in [
        os.environ.get("ARTIFICIAL_ANALYSIS_API_KEY"),
        os.environ.get("DISCORD_WEBHOOK_URL"),
        os.environ.get("OCI_NAMESPACE"),
        os.environ.get("OCI_BUCKET"),
    ] if v
]


def notify_success(file_count: int) -> None:
    send_embed({
        "embeds": [{
            "title":       "✅ AI 모델 raw 데이터 수집 완료",
            "color":       0x57F287,
            "description": f"raw 파일 {file_count}개가 OCI에 업로드됐습니다.",
            "footer":      {"text": now_display()},
        }]
    })


def notify_failure(step_name: str, exc: Exception) -> None:
    send_error(f"AI 파이프라인 실패 — {step_name}", exc, _SENSITIVE_VALUES)