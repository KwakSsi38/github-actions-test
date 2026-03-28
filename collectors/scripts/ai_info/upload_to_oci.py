"""
upload_to_oci.py — 결과물 OCI Object Storage 업로드

업로드 대상 (config.OCI_UPLOAD_TARGETS):
  - data/ai-info/integrated_major_models.json
  - data/ai-info/model_benchmarks_records.json
  - data/ai-info/category_stats.json

성공/실패 여부를 Discord로 알림.
"""

import logging
import sys

from collectors.scripts.ai_info.config import OCI_UPLOAD_TARGETS
from collectors.scripts.ai_info.notify import notify_failure, notify_success
from collectors.scripts.ai_info.oci_manager import OciManager
from collectors.scripts.ai_info.utils import load_json, setup_logging

logger = logging.getLogger(__name__)


def main() -> None:
    setup_logging()
    logger.info("OCI 업로드 시작 (대상 %d개)", len(OCI_UPLOAD_TARGETS))

    manager            = OciManager()
    succeeded: list[str] = []
    failed: list[str]    = []

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

    # 통계 요약 수집 (Discord 알림용)
    try:
        from collectors.scripts.ai_info.config import AI_INFO_DIR, RANKINGS_DIR
        integrated = load_json(AI_INFO_DIR / "integrated_major_models.json")
        records    = load_json(RANKINGS_DIR / "model_benchmarks_records.json")
        vendor_count = len(integrated) if isinstance(integrated, list) else 0
        model_count  = sum(
            len(f.get("models", []))
            for v in (integrated if isinstance(integrated, list) else [])
            for f in v.get("families", [])
        )
        record_count = len(records) if isinstance(records, list) else 0
        notify_success(model_count, vendor_count, record_count)
    except Exception as e:
        logger.warning("Discord 알림 통계 수집 실패 (무시): %s", e)

    logger.info("OCI 업로드 완료 (%d개)", len(succeeded))


if __name__ == "__main__":
    main()
