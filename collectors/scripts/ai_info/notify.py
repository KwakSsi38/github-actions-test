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


def notify_failure(
        step_name: str,
        exc: Exception,
        succeeded: list[str] | None = None,
        failed: list[str] | None = None,
) -> None:
    """
    파이프라인 실패 알림.

    Args:
        step_name: 실패한 단계명 (예: "upload_to_oci")
        exc:       발생한 예외
        succeeded: 성공한 파일 목록 (upload_to_oci 단계에서 부분 실패 시 사용)
        failed:    실패한 파일 목록
    """
    detail_lines = []
    if succeeded:
        detail_lines.append(f"✓ 성공: {', '.join(succeeded)}")
    if failed:
        detail_lines.append(f"✗ 실패: {', '.join(failed)}")

    detail = "\n".join(detail_lines)
    wrapped = RuntimeError(f"{exc}\n{detail}") if detail else exc

    send_error(f"AI 파이프라인 실패 — {step_name}", wrapped, _SENSITIVE_VALUES)