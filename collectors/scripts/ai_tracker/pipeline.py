"""
ai_tracker/pipeline.py — AI Tracker 파이프라인 진입점

collector.py(수집) → oci_manager.py(업로드)를 조합해 전체 흐름을 실행합니다.
GitHub Actions에서 이 모듈을 직접 실행합니다.

  python -m collectors.scripts.ai_tracker.pipeline
"""

import logging
import sys

from collectors.scripts.ai_tracker.collector import build_json, collect_all
from collectors.scripts.ai_tracker.notify import notify_failure, notify_success
from collectors.scripts.ai_tracker.oci_manager import AiTrackerOciManager
from collectors.scripts.shared.utils import setup_logging

setup_logging()
log: logging.Logger = logging.getLogger(__name__)


def main() -> None:
    # 1. 수집
    log.info("=== AI Tracker 파이프라인 시작 ===")
    items = collect_all()
    if not items:
        exc = RuntimeError("수집된 항목이 없습니다.")
        log.error(str(exc))
        notify_failure("수집 실패", exc)
        sys.exit(1)

    # 2. 직렬화
    content: bytes = build_json(items).encode("utf-8")

    # 3. OCI 업로드
    manager = AiTrackerOciManager()
    etag = manager.upload_updates(content)

    if etag is None:
        exc = RuntimeError("OCI 업로드 최종 실패")
        log.error(str(exc))
        notify_failure("OCI 업로드 실패", exc)
        sys.exit(1)

    log.info("파이프라인 완료: %d건 업로드 (ETag: %s)", len(items), etag)
    notify_success(len(items))


if __name__ == "__main__":
    main()
