import pytest
import json
from unittest.mock import patch, MagicMock

from collectors.scripts.prompts.sourcegraph import collect_repos

@patch("collectors.scripts.prompts.sourcegraph.requests.get")
@patch("collectors.scripts.prompts.sourcegraph.SOURCEGRAPH_TOKEN", "test_token")
def test_collect_repos_success(mock_get: MagicMock) -> None:
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.__enter__.return_value = mock_response
    mock_response.iter_lines.return_value = [
        "event: matches",
        'data: [{"type": "path", "repository": "github.com/test/repo1", "path": "skill.md"}]',
        "",
        "event: done",
        "data: {}",
    ]
    mock_get.return_value = mock_response

    results = collect_repos()
    assert len(results) == 1
    assert "test/repo1" in results
    assert results["test/repo1"] == [{"file_path": "skill.md"}]

@patch("collectors.scripts.prompts.sourcegraph.SOURCEGRAPH_TOKEN", "")
def test_collect_repos_no_token() -> None:
    with pytest.raises(SystemExit):
        collect_repos()

@patch("collectors.scripts.prompts.sourcegraph.requests.get")
@patch("collectors.scripts.prompts.sourcegraph.SOURCEGRAPH_TOKEN", "test_token")
def test_collect_repos_request_exception(mock_get: MagicMock) -> None:
    import requests
    mock_response = MagicMock()
    mock_response.status_code = 401
    mock_response.text = "Unauthorized"
    mock_response.__enter__.return_value = mock_response
    mock_get.return_value = mock_response
    
    with pytest.raises(RuntimeError):
        collect_repos()