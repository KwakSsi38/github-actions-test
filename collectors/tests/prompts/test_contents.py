import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock

from collectors.scripts.prompts.contents import (
    _decode,
    _get_raw,
    _get_content,
    fetch_one,
)

def test_decode() -> None:
    # Normal decoding
    text, has_err = _decode(b"test string")
    assert text == "test string"
    assert has_err is False
    
    # Decoding error handling
    invalid_bytes = b"\xff\xfe\xfd"
    text, has_err = _decode(invalid_bytes)
    assert has_err is True
    assert isinstance(text, str)

@patch("collectors.scripts.prompts.contents.requests.get")
@patch("collectors.scripts.prompts.contents.rotator")
def test_get_raw_success(mock_rotator: MagicMock, mock_get: MagicMock) -> None:
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.text = "raw content"
    mock_get.return_value = mock_response
    mock_rotator.raw_headers.return_value = {"Auth": "Token"}
    
    content = _get_raw("http://raw-url")
    assert content == "raw content"

@patch("collectors.scripts.prompts.contents.requests.get")
@patch("collectors.scripts.prompts.contents.time.sleep")
@patch("collectors.scripts.prompts.contents.rotator")
def test_get_raw_failure(mock_rotator: MagicMock, mock_sleep: MagicMock, mock_get: MagicMock) -> None:
    import requests
    mock_get.side_effect = requests.exceptions.RequestException("error")
    
    content = _get_raw("http://raw-url")
    assert content is None
    assert mock_get.call_count == 3

@patch("collectors.scripts.prompts.contents.requests.get")
@patch("collectors.scripts.prompts.contents.rotator")
def test_get_content_success(mock_rotator: MagicMock, mock_get: MagicMock) -> None:
    import base64
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.headers = {"X-RateLimit-Remaining": "5000"}
    mock_response.json.return_value = {
        "sha": "test-sha",
        "size": 100,
        "content": base64.b64encode(b"test content").decode("utf-8")
    }
    mock_get.return_value = mock_response
    mock_rotator.headers.return_value = {"Auth": "Token"}
    
    result = _get_content("owner/repo", "skill.md", "main")
    assert result is not None
    assert result[0] == "test content"
    assert result[1] == "test-sha"
    assert result[2] is False

@patch("collectors.scripts.prompts.contents.requests.get")
@patch("collectors.scripts.prompts.contents.rotator")
@patch("collectors.scripts.prompts.contents._get_raw")
def test_get_content_large_file(mock_get_raw: MagicMock, mock_rotator: MagicMock, mock_get: MagicMock) -> None:
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.headers = {"X-RateLimit-Remaining": "5000"}
    mock_response.json.return_value = {
        "sha": "test-sha",
        "size": 2000000, # Large file
        "download_url": "http://raw-url"
    }
    mock_get.return_value = mock_response
    mock_get_raw.return_value = "large content"
    
    result = _get_content("owner/repo", "skill.md", "main")
    assert result is not None
    assert result[0] == "large content"
    assert result[1] == "test-sha"

@patch("collectors.scripts.prompts.contents.load_json")
@patch("collectors.scripts.prompts.contents.save_json")
@patch("collectors.scripts.prompts.contents._get_content")
@patch("collectors.scripts.prompts.contents.time.sleep")
def test_fetch_one_success(mock_sleep: MagicMock, mock_get_content: MagicMock, mock_save_json: MagicMock, mock_load_json: MagicMock, tmp_path: Path) -> None:
    index = {
        "repos": {
            "123": {"source_repo": "owner/repo", "filename": "123_owner_repo.json"}
        }
    }
    
    # Create dummy file to pass exists() check
    test_file = tmp_path / "123_owner_repo.json"
    test_file.touch()
    
    mock_load_json.return_value = {
        "repository": {"default_branch": "main"},
        "skills": [{"file_path": "skill.md"}]
    }
    
    # Needs to be long enough to pass MIN_CONTENT_LEN (100)
    test_content = "test content " * 10 
    mock_get_content.return_value = (test_content, "new-sha", False)
    
    result = fetch_one("123", index, tmp_path)
    assert result is True
    mock_save_json.assert_called_once()
    
    saved_data = mock_save_json.call_args[0][0]
    assert saved_data["skills"][0]["content_md"] == test_content
    assert saved_data["skills"][0]["content_hash"] == "new-sha"

def test_fetch_one_missing_repo() -> None:
    index = {"repos": {}}
    result = fetch_one("123", index, Path("/tmp"))
    assert result is False