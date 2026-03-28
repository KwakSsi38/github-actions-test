"""
prompts/main_contents.py — 화~토 contents 스케줄러

실행 주기: 화~토 0시 / 6시 / 12시 / 18시 (5시간 40분)

흐름:
  1. lock 획득
  2. index.json 로드
  3. content_pending 큐 확인 → 비어있으면 조기 종료
  4. OCI에서 미처리 파일 다운로드
  5. content_pending 큐 이어서 처리
     - SHA 비교로 변경된 파일만 재수집
     - CHECKPOINT_N개마다 index.json 중간 저장
  6. 최종 index.json 저장 + Discord 알림
"""

import logging
import shutil
import sys
import time
from datetime import datetime, timezone

from collectors.scripts.prompts.config import CHECKPOINT_N, MAX_RUNTIME_SEC, WORK_DIR
from collectors.scripts.prompts.contents import fetch_one
from collectors.scripts.prompts.notify import (
    notify_complete,
    notify_error,
    notify_fail_warning,
)
from collectors.scripts.prompts.oci_manager import OciManager
from collectors.scripts.shared.utils import setup_logging

logger = logging.getLogger(__name__)


def main() -> None:
    setup_logging()
    start_time = time.time()
    WORK_DIR.mkdir(exist_ok=True)
    oci = OciManager()

    if not oci.acquire_lock():
        logger.error("Lock 획득 실패 — 이전 실행 진행 중. 종료합니다.")
        sys.exit(0)

    try:
        _run(oci, start_time)
    except Exception as e:
        logger.exception("contents 스케줄러 비정상 종료")
        notify_error(e)
        raise
    finally:
        oci.release_lock()
        _cleanup()


def _run(oci: OciManager, start_time: float) -> None:
    index = oci.load_index()
    q     = index["queue"]
    logger.info("content_pending: %d개", len(q["content_pending"]))

    if not q["content_pending"]:
        logger.info("content_pending 큐가 비어있습니다. 종료합니다.")
        notify_complete({"updated": 0, "skipped": 0, "failed": 0}, 0)
        return

    # OCI에서 미처리 파일 다운로드
    _download_pending(oci, index, q["content_pending"])

    logger.info("=== contents 처리 시작 ===")
    stats   = {"updated": 0, "skipped": 0, "failed": 0}
    pending = list(q["content_pending"])
    total   = len(pending)
    now     = datetime.now(timezone.utc).isoformat()

    for i, gid_str in enumerate(pending, 1):
        if time.time() - start_time > MAX_RUNTIME_SEC:
            logger.warning("⏰ 시간 초과 — %d/%d 처리 후 중단", i - 1, total)
            break

        meta = index["repos"].get(gid_str, {})
        logger.info("[%d/%d] %s", i, total, meta.get("source_repo", gid_str))

        try:
            if not fetch_one(gid_str, index, WORK_DIR):
                stats["failed"] += 1
                notify_fail_warning(stats["failed"], total)
                continue

            filename = meta.get("filename")
            oci_etag = oci.upload_file(filename, WORK_DIR) if filename else None

            if oci_etag:
                index["repos"][gid_str].update({
                    "oci_etag":       oci_etag,
                    "content_status": "done",
                    "last_content":   now,
                })
                q["content_pending"].remove(gid_str)
                stats["updated"] += 1
            else:
                stats["failed"] += 1
                notify_fail_warning(stats["failed"], total)

        except Exception as e:
            logger.error("  ✗ %s: %s", gid_str, e)
            stats["failed"] += 1
            notify_fail_warning(stats["failed"], total)

        if i % CHECKPOINT_N == 0:
            logger.info("  💾 체크포인트 (%d/%d)", i, total)
            oci.save_index(index, checkpoint=True)

    elapsed     = time.time() - start_time
    interrupted = bool(q["content_pending"])

    oci.save_index(index, stats=stats, elapsed_sec=elapsed, checkpoint=False)
    notify_complete(stats, elapsed, interrupted=interrupted)

    status = "중단 (다음 실행에서 재개)" if interrupted else "완료"
    logger.info(
        "=== %s === updated:%d skipped:%d failed:%d",
        status, stats["updated"], stats["skipped"], stats["failed"],
    )


def _download_pending(oci: OciManager, index: dict, pending: list) -> None:
    """content_pending 중 work_dir에 없는 파일을 OCI에서 다운로드."""
    WORK_DIR.mkdir(exist_ok=True)
    downloaded = 0
    for gid_str in pending:
        filename = index["repos"].get(gid_str, {}).get("filename")
        if filename and oci.download_file(filename, WORK_DIR):
            downloaded += 1
    if downloaded:
        logger.info("OCI에서 %d개 파일 다운로드 완료", downloaded)


def _cleanup() -> None:
    if WORK_DIR.exists():
        shutil.rmtree(WORK_DIR)
        logger.info("work_dir 정리: %s", WORK_DIR)


if __name__ == "__main__":
    main()
