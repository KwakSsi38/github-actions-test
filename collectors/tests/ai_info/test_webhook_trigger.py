"""
test_webhook_trigger.py — webhook_trigger.main() 단위 테스트

검증 대상:
  - 환경변수 누락 시 sys.exit(1)
  - Webhook 성공 (HTTP 200)
  - Webhook 실패 (non-200)
  - 네트워크 오류 (httpx.RequestError)
"""

import pytest
from unittest.mock import patch, MagicMock

import httpx

from collectors.scripts.ai_info.webhook_trigger import main


@patch("collectors.scripts.ai_info.webhook_trigger.setup_logging")
def test_missing_webhook_url_exits(mock_setup: MagicMock) -> None:
    """SPRING_WEBHOOK_URL 누락 시 sys.exit(1)해야 한다."""
    with patch.dict("os.environ", {"SPRING_WEBHOOK_URL": "", "SPRING_WEBHOOK_SECRET": "secret"}, clear=False):
        with pytest.raises(SystemExit):
            main()


@patch("collectors.scripts.ai_info.webhook_trigger.setup_logging")
def test_missing_webhook_secret_exits(mock_setup: MagicMock) -> None:
    """SPRING_WEBHOOK_SECRET 누락 시 sys.exit(1)해야 한다."""
    with patch.dict("os.environ", {"SPRING_WEBHOOK_URL": "http://spring.test/webhook", "SPRING_WEBHOOK_SECRET": ""}, clear=False):
        with pytest.raises(SystemExit):
            main()


@patch("collectors.scripts.ai_info.webhook_trigger.setup_logging")
@patch("collectors.scripts.ai_info.webhook_trigger.httpx.Client")
def test_webhook_success(mock_client_class: MagicMock, mock_setup: MagicMock) -> None:
    """HTTP 200 응답 시 정상 종료해야 한다."""
    mock_client = MagicMock()
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = "ok"
    mock_client.post.return_value = mock_resp
    mock_client_class.return_value.__enter__.return_value = mock_client

    env = {"SPRING_WEBHOOK_URL": "http://spring.test/webhook", "SPRING_WEBHOOK_SECRET": "secret"}
    with patch.dict("os.environ", env, clear=False):
        main()  # 예외 없이 종료되어야 한다


@patch("collectors.scripts.ai_info.webhook_trigger.setup_logging")
@patch("collectors.scripts.ai_info.webhook_trigger.httpx.Client")
def test_webhook_non_200_exits(mock_client_class: MagicMock, mock_setup: MagicMock) -> None:
    """non-200 응답 시 sys.exit(1)해야 한다."""
    mock_client = MagicMock()
    mock_resp = MagicMock()
    mock_resp.status_code = 500
    mock_resp.text = "Internal Server Error"
    mock_client.post.return_value = mock_resp
    mock_client_class.return_value.__enter__.return_value = mock_client

    env = {"SPRING_WEBHOOK_URL": "http://spring.test/webhook", "SPRING_WEBHOOK_SECRET": "secret"}
    with patch.dict("os.environ", env, clear=False):
        with pytest.raises(SystemExit):
            main()


@patch("collectors.scripts.ai_info.webhook_trigger.setup_logging")
@patch("collectors.scripts.ai_info.webhook_trigger.httpx.Client")
def test_webhook_network_error_exits(mock_client_class: MagicMock, mock_setup: MagicMock) -> None:
    """네트워크 오류(httpx.RequestError) 시 sys.exit(1)해야 한다."""
    mock_client = MagicMock()
    mock_client.post.side_effect = httpx.RequestError("connection refused")
    mock_client_class.return_value.__enter__.return_value = mock_client

    env = {"SPRING_WEBHOOK_URL": "http://spring.test/webhook", "SPRING_WEBHOOK_SECRET": "secret"}
    with patch.dict("os.environ", env, clear=False):
        with pytest.raises(SystemExit):
            main()
