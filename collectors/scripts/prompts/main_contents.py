"""
prompts/main_contents.py — 화~토 contents 스케줄러

실행 주기: 화~토 0시 / 6시 / 12시 / 18시 (6시간, 5시간 40분부터 종료 준비)

흐름 (리팩토링됨):
  1. lock 획득
  2. index.json 로드
  3. content_pending 큐 확인 → 비어있으면 조기 종료
  4. content_pending 큐 순회 처리 (Chunk / Lazy Loading 방식)
     - 대상 파일을 OCI에서 단건 다운로드
     - SHA 비교로 변경된 파일만 재수집
     - OCI에 수정된 파일 단건 업로드
     - 로컬 파일 즉시 삭제 (디스크 누수 방지)
     - CHECKPOINT_N개마다 index.json 중간 저장
  5. 최종 index.json 저장 + Discord 알림
"""

import logging
import shutil
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

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
        notify_complete({"updated": 0, "skipped": 0, "failed": 0}, 0.0)
        return

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
        filename = meta.get("filename")
        logger.info("[%d/%d] %s", i, total, meta.get("source_repo", gid_str))

        if not filename:
            logger.warning("  ✗ filename 정보 없음: %s", gid_str)
            stats["failed"] += 1
            notify_fail_warning(stats["failed"], total)
            continue

        local_path = WORK_DIR / filename

        try:
            # 단건 지연 로딩
            if not oci.download_file(filename, WORK_DIR):
                logger.warning("  ✗ OCI 다운로드 실패: %s", filename)
                stats["failed"] += 1
                notify_fail_warning(stats["failed"], total)
                continue

            # 상태 문자열 반환 받기
            fetch_status = fetch_one(gid_str, index, WORK_DIR)

            if fetch_status == "failed":
                stats["failed"] += 1
                notify_fail_warning(stats["failed"], total)
                continue

            elif fetch_status == "skipped":
                # OCI 업로드 건너뜀 (핵심 병목 해결)
                index["repos"][gid_str]["content_status"] = "done"
                index["repos"][gid_str]["last_content"] = now
                q["content_pending"].remove(gid_str)
                stats["skipped"] += 1
                continue

            elif fetch_status == "updated":
                oci_etag = oci.upload_file(filename, WORK_DIR)
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

        finally:
            # 디스크 누수 방지용 Cleanup
            if local_path.exists():
                local_path.unlink(missing_ok=True)

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


def _cleanup() -> None:
    if WORK_DIR.exists():
        shutil.rmtree(WORK_DIR)
        logger.info("work_dir 정리: %s", WORK_DIR)


if __name__ == "__main__":
    main()