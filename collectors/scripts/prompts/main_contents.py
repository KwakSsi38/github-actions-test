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

import sys
from pathlib import Path

# ── 1. 로컬 환경변수 로드 (.env) ──────────────────────────────────────────────
# 주의: config.py 등에서 os.environ을 임포트 시점에 평가하므로,
# 프로젝트 내부 모듈(collectors.scripts...) 임포트 전에 먼저 dotenv를 로드해야 합니다.
try:
    from dotenv import load_dotenv
    env_path = Path(__file__).resolve().parent.parent.parent / ".env"
except ImportError:
    pass  # GitHub Actions 등 CI/CD 환경에서 패키지가 없는 경우 무시


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
            # 1. 지연 로딩 (Lazy Download): 필요한 파일 1개만 다운로드
            if not oci.download_file(filename, WORK_DIR):
                logger.warning("  ✗ OCI 다운로드 실패: %s", filename)
                stats["failed"] += 1
                notify_fail_warning(stats["failed"], total)
                continue

            # 2. 내용 수집 및 로컬 파일 업데이트
            if not fetch_one(gid_str, index, WORK_DIR):
                stats["failed"] += 1
                notify_fail_warning(stats["failed"], total)
                continue

            # 3. 원자적 업로드 처리
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
            # 4. 디스크 누수 방지 (Cleanup): 처리 완료 또는 실패 시 파일 즉시 삭제
            if local_path.exists():
                local_path.unlink(missing_ok=True)

        # 5. 체크포인트 저장
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