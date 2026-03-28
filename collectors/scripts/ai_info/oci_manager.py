"""
ai_info/oci_manager.py — ai_info 파이프라인 OCI 클라이언트

shared/oci_client.py의 BaseOciClient를 상속.
index.json / lock.json 없이 단순 파일 업로드만 사용.
"""

import logging
from pathlib import Path

from collectors.scripts.shared.oci_client import BaseOciClient

logger = logging.getLogger(__name__)


class OciManager(BaseOciClient):
    def upload_file(self, object_name: str, content: bytes) -> str | None:
        """OCI에 파일 업로드. 성공 시 ETag, 실패 시 None."""
        return self.put_object(object_name, content)
