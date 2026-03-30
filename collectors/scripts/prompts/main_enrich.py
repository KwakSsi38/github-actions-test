"""
prompts/main_enrich.py — 월요일 enrich 스케줄러

실행 주기: 월요일 0시 / 6시 / 12시 / 18시 (6시간, 5시간 40분부터 종료 준비)

흐름:
  1. lock 획득
  2. index.json 로드
  3. [0시만] Sourcegraph 수집 → enrich_pending 큐 갱신 (workflow에서 받은 값에 따라 결정)
  4. enrich_pending 큐 이어서 처리
     - 완료 즉시 content_pending 큐에 추가
     - CHECKPOINT_N개마다 index.json 중간 저장
  5. 큐 비어있으면 조기 종료
  6. 최종 index.json 저장 + Discord 알림
"""

import logging
import os
import shutil
import sys
import time
from datetime import datetime, timezone

from collectors.scripts.prompts.config import (
    CHECKPOINT_N,
    MAX_RUNTIME_SEC,
    WORK_DIR,
)
from collectors.scripts.prompts.enrich import enrich_one
from collectors.scripts.prompts.notify import (
    notify_complete,
    notify_error,
    notify_fail_warning,
    notify_start,
)
from collectors.scripts.prompts.oci_manager import OciManager
from collectors.scripts.prompts.sourcegraph import collect_repos
from collectors.scripts.shared.utils import load_json, setup_logging

logger = logging.getLogger(__name__)

IS_MONDAY_MIDNIGHT = os.environ.get("IS_MONDAY_MIDNIGHT", "false").lower() == "true"


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
        logger.exception("enrich 스케줄러 비정상 종료")
        notify_error(e)
        raise
    finally:
        oci.release_lock()
        _cleanup()


def _run(oci: OciManager, start_time: float) -> None:
    index = oci.load_index()
    q     = index["queue"]
    logger.info(
        "enrich_pending: %d개 / content_pending: %d개",
        len(q["enrich_pending"]), len(q["content_pending"]),
    )

    # ── Sourcegraph 수집 (0시 실행만) ─────────────────────────────────────────
    new_count = removed_count = 0
    if IS_MONDAY_MIDNIGHT:
        new_count, removed_count = _collect_and_enqueue(oci, index)

    if not q["enrich_pending"]:
        logger.info("enrich_pending 큐가 비어있습니다. 종료합니다.")
        return

    # ── enrich 처리 ───────────────────────────────────────────────────────────
    logger.info("=== enrich 처리 시작 ===")
    stats      = {"new": new_count, "updated": 0, "skipped": 0, "failed": 0, "removed": removed_count}
    new_failed = {}
    pending    = list(q["enrich_pending"])
    total      = len(pending)

    # indexed 맵은 루프 전에 한 번만 생성
    indexed = {meta["source_repo"]: gid for gid, meta in index["repos"].items()}

    for i, source_repo in enumerate(pending, 1):
        if time.time() - start_time > MAX_RUNTIME_SEC:
            logger.warning("⏰ 시간 초과 — %d/%d 처리 후 중단", i - 1, total)
            break

        logger.info("[%d/%d] %s", i, total, source_repo)

        gid_str     = indexed.get(source_repo)
        stored_etag = index["repos"].get(gid_str, {}).get("etag") if gid_str else None
        result      = None

        # ── file_entries 조회: 신규는 pending_file_entries, 기존은 work_dir에서 복원 ──
        file_entries = index["pending_file_entries"].get(source_repo)
        if file_entries is None and gid_str:
            file_entries = _restore_file_entries(source_repo, gid_str, index, oci)

        file_entries = file_entries or []

        try:
            result = enrich_one(
                source_repo=source_repo,
                file_entries=file_entries,
                index=index,
                work_dir=WORK_DIR,
                stored_etag=stored_etag,
            )

            if result is None:
                stats["failed"] += 1
                new_failed[source_repo] = {"reason": "enrich 실패"}
            else:
                # notify_complete가 읽는 키(new/updated/skipped)로 분기
                if result["changed"]:
                    if gid_str:
                        stats["updated"] += 1
                    else:
                        stats["new"] += 1
                else:
                    stats["skipped"] += 1
                q["enrich_pending"].remove(source_repo)
                index["pending_file_entries"].pop(source_repo, None)  # 임시 저장 정리
                index["failed_repos"].pop(source_repo, None)

            notify_fail_warning(stats["failed"], total)

        except Exception as e:
            logger.error("  ✗ %s: %s", source_repo, e)
            stats["failed"] += 1
            new_failed[source_repo] = {"reason": str(e)}

        # OCI 업로드
        if result and result.get("changed") and result.get("filename"):
            oci_etag = oci.upload_file(result["filename"], WORK_DIR)
            if oci_etag and result.get("github_id"):
                index["repos"][result["github_id"]]["oci_etag"] = oci_etag

        if i % CHECKPOINT_N == 0:
            index["failed_repos"].update(new_failed)
            logger.info("  💾 체크포인트 (%d/%d)", i, total)
            oci.save_index(index, checkpoint=True)

    # ── 최종 저장 ─────────────────────────────────────────────────────────────
    index["failed_repos"].update(new_failed)
    elapsed     = time.time() - start_time
    interrupted = bool(q["enrich_pending"])

    oci.save_index(index, stats=stats, elapsed_sec=elapsed, checkpoint=False)
    notify_complete(stats, elapsed, interrupted=interrupted)

    status = "중단 (다음 실행에서 재개)" if interrupted else "완료"
    logger.info(
        "=== %s === new:%d updated:%d skipped:%d failed:%d",
        status, stats["new"], stats["updated"], stats["skipped"], stats["failed"],
    )


def _restore_file_entries(
        source_repo: str, gid_str: str, index: dict, oci: OciManager
) -> list[dict] | None:
    """기존 레포의 file_entries를 work_dir JSON에서 복원."""
    filename   = index["repos"].get(gid_str, {}).get("filename")
    local_path = WORK_DIR / filename if filename else None

    if local_path and not local_path.exists():
        oci.download_file(filename, WORK_DIR)

    if local_path and local_path.exists():
        existing_data = load_json(local_path)
        file_entries  = [
            {"file_path": s["file_path"]}
            for s in existing_data.get("skills", [])
        ]
        if file_entries:
            logger.info("  → file_entries 복원 완료: %d개", len(file_entries))
            return file_entries

    logger.warning("  → file_entries 복원 실패 (skills 없음): %s", source_repo)
    return None


def _collect_and_enqueue(oci: OciManager, index: dict) -> tuple[int, int]:
    """Sourcegraph 수집 후 enrich_pending 큐 갱신."""
    logger.info("=== Sourcegraph 수집 ===")
    sg_repos = collect_repos()
    index["last_sourcegraph_run"] = datetime.now(timezone.utc).isoformat()

    q               = index["queue"]
    indexed_by_repo = {meta["source_repo"]: gid for gid, meta in index["repos"].items()}
    new_count = removed_count = 0

    for source_repo, file_entries in sg_repos.items():
        if source_repo not in indexed_by_repo:
            # 신규 레포: file_entries를 pending_file_entries에 저장
            if source_repo not in q["enrich_pending"]:
                q["enrich_pending"].append(source_repo)
                index["pending_file_entries"][source_repo] = file_entries
            new_count += 1
        elif source_repo not in q["enrich_pending"]:
            # 기존 레포: 큐에만 추가 (file_entries는 _run에서 work_dir JSON으로 복원)
            q["enrich_pending"].append(source_repo)

    sg_repos_set = set(sg_repos.keys())
    for gid, meta in index["repos"].items():
        if meta["source_repo"] not in sg_repos_set and meta.get("active", True):
            index["repos"][gid]["active"] = False
            removed_count += 1

    logger.info(
        "신규: %d개 / 사라진: %d개 / enrich_pending 총: %d개",
        new_count, removed_count, len(q["enrich_pending"]),
    )
    notify_start(new_count, len(indexed_by_repo), removed_count)
    oci.save_index(index, checkpoint=True)

    return new_count, removed_count


def _cleanup() -> None:
    if WORK_DIR.exists():
        shutil.rmtree(WORK_DIR)
        logger.info("work_dir 정리: %s", WORK_DIR)


if __name__ == "__main__":
    main()