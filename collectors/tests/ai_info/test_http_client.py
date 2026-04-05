import pytest
import sys
from unittest.mock import patch, MagicMock

import httpx
from tenacity import RetryError

from collectors.scripts.ai_info.http_client import fetch_json, _ClientError


@patch("collectors.scripts.ai_info.http_client.httpx.Client")
def test_fetch_json_success(mock_client_class: MagicMock) -> None:
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {"key": "value"}
    mock_client.get.return_value = mock_response
    mock_client_class.return_value.__enter__.return_value = mock_client

    result = fetch_json("http://test.com")
    assert result == {"key": "value"}


@patch("collectors.scripts.ai_info.http_client.httpx.Client")
def test_fetch_json_4xx_exits_without_retry(mock_client_class: MagicMock) -> None:
    """4xx 응답은 _ClientError로 변환되어 재시도 없이 즉시 sys.exit(1)해야 한다."""
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 404
    # 4xx는 raise_for_status()에 도달하지 않으므로 side_effect 없음
    mock_response.raise_for_status.side_effect = None
    mock_client.get.return_value = mock_response
    mock_client_class.return_value.__enter__.return_value = mock_client

    with pytest.raises(SystemExit):
        fetch_json("http://test.com")


@patch("collectors.scripts.ai_info.http_client.httpx.Client")
def test_fetch_json_401_exits_without_retry(mock_client_class: MagicMock) -> None:
    """401 인증 오류도 재시도 없이 즉시 sys.exit(1)해야 한다."""
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 401
    mock_client.get.return_value = mock_response
    mock_client_class.return_value.__enter__.return_value = mock_client

    with pytest.raises(SystemExit):
        fetch_json("http://test.com")


@patch("collectors.scripts.ai_info.http_client._get_with_retry")
def test_fetch_json_retry_error(mock_get_with_retry: MagicMock) -> None:
    """최대 재시도 횟수 초과 시 sys.exit(1)해야 한다."""
    mock_get_with_retry.side_effect = RetryError(MagicMock())

    with pytest.raises(SystemExit):
        fetch_json("http://test.com")


@patch("collectors.scripts.ai_info.http_client._get_with_retry")
def test_fetch_json_other_error(mock_get_with_retry: MagicMock) -> None:
    """예상치 못한 예외도 sys.exit(1)해야 한다."""
    mock_get_with_retry.side_effect = Exception("Some other error")

    with pytest.raises(SystemExit):
        fetch_json("http://test.com")


@patch("collectors.scripts.ai_info.http_client._get_with_retry")
def test_fetch_json_client_error_directly(mock_get_with_retry: MagicMock) -> None:
    """_ClientError가 fetch_json까지 전파되면 sys.exit(1)해야 한다."""
    mock_get_with_retry.side_effect = _ClientError("클라이언트 오류 403: http://test.com")

    with pytest.raises(SystemExit):
        fetch_json("http://test.com")