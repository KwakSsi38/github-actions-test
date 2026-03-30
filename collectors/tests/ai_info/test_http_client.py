import pytest
import sys
from unittest.mock import patch, MagicMock

import httpx
from tenacity import RetryError

from collectors.scripts.ai_info.http_client import fetch_json

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
def test_fetch_json_client_error(mock_client_class: MagicMock) -> None:
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.status_code = 404
    mock_client.get.return_value = mock_response
    mock_client_class.return_value.__enter__.return_value = mock_client

    with pytest.raises(SystemExit):
        fetch_json("http://test.com")

@patch("collectors.scripts.ai_info.http_client._get_with_retry")
def test_fetch_json_retry_error(mock_get_with_retry: MagicMock) -> None:
    mock_get_with_retry.side_effect = RetryError(MagicMock())
    
    with pytest.raises(SystemExit):
        fetch_json("http://test.com")

@patch("collectors.scripts.ai_info.http_client._get_with_retry")
def test_fetch_json_other_error(mock_get_with_retry: MagicMock) -> None:
    mock_get_with_retry.side_effect = Exception("Some other error")
    
    with pytest.raises(SystemExit):
        fetch_json("http://test.com")