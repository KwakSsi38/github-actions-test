"""
upload_to_oci.py — raw 데이터 OCI Object Storage 업로드

업로드 대상:
  - data/ai-info/models_info_raw.json
  - data/ai-info/models_benchmark_raw.json

가공/매칭/통계는 Java(Spring)에서 처리.
성공/실패 여부를 Discord로 알림.
"""

import logging
import sys

from collectors.scripts.ai_info.config import OCI_UPLOAD_TARGETS
from collectors.scripts.ai_info.notify import notify_failure, notify_success
from collectors.scripts.ai_info.oci_manager import OciManager
from collectors.scripts.shared.utils import setup_logging

logger = logging.getLogger(__name__)


def main() -> None:
    setup_logging()
    logger.info("OCI 업로드 시작 (대상 %d개)", len(OCI_UPLOAD_TARGETS))

    manager   = OciManager()
    succeeded = []
    failed    = []

    for local_path, object_name in OCI_UPLOAD_TARGETS:
        if not local_path.exists():
            logger.error("로컬 파일 없음 (이전 단계 실패 가능성): %s", local_path)
            failed.append(object_name)
            continue

        oci_etag = manager.upload_file(object_name, local_path.read_bytes())

        if oci_etag:
            logger.info("✓ 업로드 완료: %s (etag=%s)", object_name, oci_etag)
            succeeded.append(object_name)
        else:
            logger.error("✗ 업로드 실패: %s", object_name)
            failed.append(object_name)

    if failed:
        logger.error("업로드 실패 파일: %s", failed)
        try:
            notify_failure("upload_to_oci", RuntimeError(f"업로드 실패: {failed}"))
        except Exception:
            pass
        sys.exit(1)

    notify_success(len(succeeded))
    logger.info("OCI 업로드 완료 (%d개)", len(succeeded))


if __name__ == "__main__":
    main()