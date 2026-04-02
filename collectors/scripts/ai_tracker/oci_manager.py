"""
ai_tracker/oci_manager.py — AI Tracker OCI 업로드 클라이언트

shared/oci_client.py의 BaseOciClient를 상속.
updates.json 단일 파일 업로드만 담당.

OCI object name: ai-tracker/updates.json
  - 매 실행마다 덮어쓰기 (latest 단일 파일 유지)
  - Spring Boot가 webhook 수신 후 이 파일을 다운로드해 가공합니다.

재시도: shared/oci_client.py BaseOciClient.put_object 내부에서
        최대 3회, UPLOAD_DELAY 간격으로 처리합니다.
"""

from collectors.scripts.shared.oci_client import BaseOciClient

OBJECT_NAME: str = "data/ai-tracker/updates.json"


class AiTrackerOciManager(BaseOciClient):
    """ai_tracker 파이프라인 OCI 업로더."""

    def upload_updates(self, content: bytes) -> str | None:
        """updates.json을 OCI에 업로드합니다. 성공 시 ETag, 실패 시 None."""
        return self.put_object(OBJECT_NAME, content)