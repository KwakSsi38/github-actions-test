"""
prompts/oci_manager.py — prompts 파이프라인 OCI 관리자

shared/oci_client.py의 BaseOciClient를 상속해
index.json / lock.json 관리 기능을 추가.
"""

import json
import logging
import time
from pathlib import Path

from oci.exceptions import ServiceError

from collectors.scripts.prompts.config import (
    LOCK_TTL_SEC,
    OCI_DATA_PREFIX,
    OCI_INDEX_OBJECT,
    OCI_LOCK_OBJECT,
)
from collectors.scripts.shared.oci_client import BaseOciClient
from collectors.scripts.shared.utils import now_iso

logger = logging.getLogger(__name__)


class OciManager(BaseOciClient):

    # ── 동시 실행 방지 ─────────────────────────────────────────────────────────
    def acquire_lock(self) -> bool:
        content = self.get_object_bytes(OCI_LOCK_OBJECT)
        if content:
            lock = json.loads(content)
            if time.time() - lock.get("locked_at", 0) < LOCK_TTL_SEC:
                logger.warning("Lock 보유 중 (locked_at=%s)", lock.get("locked_at_iso"))
                return False
            logger.warning("Lock TTL 초과 — 강제 해제 후 재획득")

        self.put_object(
            OCI_LOCK_OBJECT,
            json.dumps({"locked_at": time.time(), "locked_at_iso": now_iso()}).encode(),
        )
        logger.info("Lock 획득 완료")
        return True

    def release_lock(self) -> None:
        try:
            self.delete_object(OCI_LOCK_OBJECT)
            logger.info("Lock 해제 완료")
        except Exception as e:
            logger.warning("Lock 해제 실패 (무시): %s", e)

    # ── 인덱스 로드 ────────────────────────────────────────────────────────────
    def load_index(self) -> dict:
        content = self.get_object_bytes(OCI_INDEX_OBJECT)
        if content:
            index = json.loads(content)
            # 구 스키마 호환: pending_file_entries 필드 없으면 추가
            index.setdefault("pending_file_entries", {})
            return index

        logger.info("index.json 없음 — 새로 생성")
        return {
            "last_sourcegraph_run": None,
            "last_updated":         None,
            "stats":                {},
            "run_history":          [],
            "queue": {
                "enrich_pending":  [],
                "content_pending": [],
            },
            "repos":                {},
            "pending_file_entries": {},  # 신규 레포 file_entries 임시 저장
            "failed_repos":         {},  # enrich 실패 레포 관리
        }

    # ── 레포 JSON 업로드 ───────────────────────────────────────────────────────
    def upload_file(self, filename: str, work_dir: Path) -> str | None:
        local_path = work_dir / filename
        if not local_path.exists():
            logger.warning("로컬 파일 없음: %s", filename)
            return None
        object_name = f"{OCI_DATA_PREFIX}{filename}"
        oci_etag    = self.put_object(object_name, local_path.read_bytes())
        if oci_etag:
            logger.debug("✓ 업로드: %s", object_name)
        return oci_etag

    # ── 레포 JSON 다운로드 ─────────────────────────────────────────────────────
    def download_file(self, filename: str, work_dir: Path) -> bool:
        local_path  = work_dir / filename
        if local_path.exists():
            return True
        content = self.get_object_bytes(f"{OCI_DATA_PREFIX}{filename}")
        if content is None:
            logger.warning("OCI 다운로드 실패 (없음): %s", filename)
            return False
        local_path.write_bytes(content)
        return True

    # ── 인덱스 저장 ────────────────────────────────────────────────────────────
    def save_index(
            self,
            index: dict,
            stats: dict | None = None,
            elapsed_sec: float = 0.0,
            history_max: int = 10,
            checkpoint: bool = False,
    ) -> None:
        now = now_iso()
        index["last_updated"] = now

        if stats:
            index["stats"] = stats

        if not checkpoint and stats:
            index.setdefault("run_history", []).append({
                "run_at":      now,
                "elapsed_sec": round(elapsed_sec),
                "stats":       stats,
            })
            index["run_history"] = index["run_history"][-history_max:]

        content = json.dumps(index, ensure_ascii=False, indent=2).encode()
        label   = "체크포인트" if checkpoint else "최종"
        if self.put_object(OCI_INDEX_OBJECT, content):
            logger.info(
                "index.json %s 저장 완료 (enrich_pending=%d content_pending=%d)",
                label,
                len(index["queue"]["enrich_pending"]),
                len(index["queue"]["content_pending"]),
            )
        else:
            logger.error("index.json %s 저장 실패!", label)