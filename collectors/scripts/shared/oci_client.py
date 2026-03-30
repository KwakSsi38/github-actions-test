"""
shared/oci_client.py — OCI Object Storage 공통 클라이언트

파이프라인(prompts, ai_info)에서 공유하는 OCI 업로드 핵심 로직.
MD5 검증 포함, 최대 N회 재시도.

파이프라인별 확장:
  - prompts:  index.json / lock.json 관리 → prompts/oci_manager.py
  - ai_info:  단순 파일 업로드만 사용    → ai_info/oci_manager.py
"""

import base64
import hashlib
import logging
import time

import oci
from oci.exceptions import ServiceError

from collectors.scripts.shared.config import (
    OCI_BUCKET,
    OCI_NAMESPACE,
    UPLOAD_DELAY,
    UPLOAD_RETRIES,
)

logger = logging.getLogger(__name__)


class BaseOciClient:
    def __init__(self):
        config = oci.config.from_file()
        self.client = oci.object_storage.ObjectStorageClient(config)

    def put_object(self, object_name: str, content: bytes,
                   content_type: str = "application/json") -> str | None:
        """
        OCI에 객체 업로드. MD5 검증 포함, 최대 N회 재시도.
        성공 시 ETag 반환, 실패 시 None.
        """
        local_md5   = hashlib.md5(content).hexdigest()
        content_md5 = base64.b64encode(bytes.fromhex(local_md5)).decode()

        for attempt in range(1, UPLOAD_RETRIES + 1):
            try:
                resp = self.client.put_object(
                    OCI_NAMESPACE, OCI_BUCKET, object_name, content,
                    content_type=content_type,
                    content_md5=content_md5,
                )
                etag = resp.headers.get("ETag", "").strip('"')
                logger.debug("✓ 업로드: %s (md5=%s)", object_name, local_md5[:8])
                return etag
            except ServiceError as e:
                logger.warning(
                    "업로드 실패 (시도 %d/%d): %s — %s",
                    attempt, UPLOAD_RETRIES, object_name, e,
                )
                if attempt < UPLOAD_RETRIES:
                    time.sleep(UPLOAD_DELAY * attempt)

        logger.error("✗ 최종 업로드 실패: %s", object_name)
        return None

    def get_object_bytes(self, object_name: str) -> bytes | None:
        """OCI에서 객체를 bytes로 다운로드. 없으면 None."""
        try:
            resp = self.client.get_object(OCI_NAMESPACE, OCI_BUCKET, object_name)
            return resp.data.content
        except ServiceError as e:
            if e.status == 404:
                return None
            raise

    def delete_object(self, object_name: str) -> None:
        """OCI 객체 삭제. 없어도 예외 무시."""
        try:
            self.client.delete_object(OCI_NAMESPACE, OCI_BUCKET, object_name)
        except ServiceError as e:
            if e.status != 404:
                raise