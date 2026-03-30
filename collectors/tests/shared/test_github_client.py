import os
import time
from unittest.mock import patch, MagicMock

import pytest
import requests

from collectors.scripts.shared.github_client import GitHubTokenRotator, github_get


def test_github_token_rotator_init_no_tokens() -> None:
    with pytest.raises(ValueError):
        GitHubTokenRotator([])


def test_github_token_rotator_headers() -> None:
    rotator = GitHubTokenRotator(["token1", "token2"])
    headers = rotator.headers()
    assert headers["Authorization"] == "token token1"
    assert headers["Accept"] == "application/vnd.github+json"
    assert headers["X-GitHub-Api-Version"] == "2022-11-28"

    rotator.rotate()
    headers = rotator.headers()
    assert headers["Authorization"] == "token token2"

    rotator.rotate()
    headers = rotator.headers()
    assert headers["Authorization"] == "token token1"


@patch("collectors.scripts.shared.github_client.requests.get")
@patch("collectors.scripts.shared.github_client.rotator", GitHubTokenRotator(["test_token"]))
def test_github_get_success(mock_get: MagicMock) -> None:
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.headers = {"X-RateLimit-Remaining": "5000"}
    mock_response.json.return_value = {"data": "test"}
    mock_get.return_value = mock_response

    status_code, body, headers = github_get("http://api.github.com")
    assert status_code == 200
    assert body == {"data": "test"}
    assert headers == {"X-RateLimit-Remaining": "5000"}


@patch("collectors.scripts.shared.github_client.requests.get")
@patch("collectors.scripts.shared.github_client.rotator", GitHubTokenRotator(["test_token"]))
@patch("collectors.scripts.shared.github_client.time.sleep")
def test_github_get_rate_limit(mock_sleep: MagicMock, mock_get: MagicMock) -> None:
    mock_response = MagicMock()
    mock_response.status_code = 403
    mock_response.headers = {
        "X-RateLimit-Remaining": "0",
        "X-RateLimit-Reset": str(int(time.time()) + 10),
    }
    mock_get.return_value = mock_response

    status_code, body, headers = github_get("http://api.github.com", retries=1)
    # the code currently retries and then fails since we only gave it 1 retry
    # actually wait it doesn't fail, it continues the loop, but since attempt loop ends, it returns -1
    assert status_code == -1
    mock_sleep.assert_called()


@patch("collectors.scripts.shared.github_client.requests.get")
@patch("collectors.scripts.shared.github_client.rotator", GitHubTokenRotator(["test_token"]))
def test_github_get_request_exception(mock_get: MagicMock) -> None:
    mock_get.side_effect = requests.exceptions.RequestException("error")
    status_code, body, headers = github_get("http://api.github.com", retries=1)
    assert status_code == -1
    assert body is None
    assert headers == {}