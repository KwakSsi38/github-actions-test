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


def notify_success(model_count: int, vendor_count: int, record_count: int) -> None:
    send_embed({
        "embeds": [{
            "title":  "✅ AI 모델 파이프라인 완료",
            "color":  0x57F287,
            "fields": [
                {"name": "벤더",     "value": str(vendor_count),  "inline": True},
                {"name": "모델",     "value": str(model_count),   "inline": True},
                {"name": "벤치마크", "value": str(record_count),  "inline": True},
            ],
            "footer": {"text": now_display()},
        }]
    })


def notify_failure(step_name: str, exc: Exception) -> None:
    send_error(f"AI 파이프라인 실패 — {step_name}", exc, _SENSITIVE_VALUES)
