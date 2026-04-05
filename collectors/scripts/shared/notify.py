"""
shared/notify.py — Discord Webhook 공통 알림 모듈

파이프라인에서 공유하는 Discord 전송 로직.
파이프라인별 알림 함수는 각자의 notify.py에서 이 모듈을 사용.
"""

import logging
import os
import traceback

import httpx

from collectors.scripts.shared.utils import now_display

logger = logging.getLogger(__name__)

DISCORD_WEBHOOK_URL = os.environ.get("DISCORD_WEBHOOK_URL", "")


def mask_sensitive(text: str, sensitive_values: list[str]) -> str:
    """민감 정보를 ***MASKED***로 치환."""
    for val in sensitive_values:
        if val:
            text = text.replace(val, "***MASKED***")
    return text


def send_embed(payload: dict) -> None:
    """Discord Webhook으로 embed 메시지 전송."""
    if not DISCORD_WEBHOOK_URL:
        logger.warning("DISCORD_WEBHOOK_URL 미설정 — 알림 스킵")
        return
    try:
        with httpx.Client(timeout=10) as client:
            resp = client.post(DISCORD_WEBHOOK_URL, json=payload)
        if resp.status_code not in (200, 204):
            logger.warning("Discord 알림 실패: HTTP %s", resp.status_code)
    except Exception as e:
        logger.warning("Discord 알림 오류: %s", e)


def send_error(title: str, exc: Exception, sensitive_values: list[str] | None = None) -> None:
    """오류 알림 전송. 스택트레이스 포함, 민감 정보 마스킹."""
    tb = "".join(traceback.format_exception(type(exc), exc, exc.__traceback__))
    if sensitive_values:
        tb = mask_sensitive(tb, sensitive_values)
    tb_short = tb[-3000:] if len(tb) > 3000 else tb
    send_embed({
        "embeds": [{
            "title":       f"🚨 {title}",
            "color":       0xED4245,
            "description": f"```\n{tb_short}\n```",
            "footer":      {"text": now_display()},
        }]
    })