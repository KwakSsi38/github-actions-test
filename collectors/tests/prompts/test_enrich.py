import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock

from collectors.scripts.prompts.enrich import (
    _parse_repo,
    _fetch_languages,
    _make_filename,
    _ensure_content_pending,
    _remove_from_content_pending,
    enrich_one,
)

def test_parse_repo() -> None:
    data = {
        "id": 12345,
        "name": "test-repo",
        "full_name": "owner/test-repo",
        "html_url": "https://github.com/owner/test-repo",
        "description": "Test description",
        "stargazers_count": 10,
        "forks_count": 5,
        "size": 100,
        "license": {"spdx_id": "MIT"},
        "homepage": "https://test.com",
        "owner": {"avatar_url": "https://avatar.com/1", "type": "User"},
        "default_branch": "main",
        "updated_at": "2023-01-01T00:00:00Z",
        "archived": False,
        "topics": ["test"],
        "visibility": "public",
        "pushed_at": "2023-01-02T00:00:00Z",
        "created_at": "2022-01-01T00:00:00Z",
        "fork": False,
    }

    parsed = _parse_repo(data)
    assert parsed["github_id"] == 12345
    assert parsed["name"] == "test-repo"
    assert parsed["source_repo"] == "owner/test-repo"
    assert parsed["license"] == "MIT"
    assert parsed["owner_type"] == "USER"
    assert parsed["active"] is True
    assert parsed["raw_metadata"]["is_fork"] is False

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
def test_fetch_languages(mock_sleep: MagicMock, mock_github_get: MagicMock) -> None:
    mock_github_get.return_value = (200, {"Python": 80, "Java": 20}, {})
    langs = _fetch_languages("owner/repo")
    assert langs == {"Python": 80.0, "Java": 20.0}

    mock_github_get.return_value = (404, None, {})
    langs_none = _fetch_languages("owner/repo2")
    assert langs_none is None

def test_make_filename() -> None:
    filename = _make_filename(123, "owner/repo")
    assert filename == "123_owner_repo.json"

def test_ensure_content_pending() -> None:
    index = {"queue": {"content_pending": []}, "repos": {"123": {}}}
    _ensure_content_pending(index, "123")
    assert "123" in index["queue"]["content_pending"]
    assert index["repos"]["123"]["content_status"] == "pending"

    # duplicate check
    _ensure_content_pending(index, "123")
    assert len(index["queue"]["content_pending"]) == 1

def test_remove_from_content_pending() -> None:
    index = {
        "queue": {"content_pending": ["123"]},
        "repos": {"123": {"content_status": "pending"}},
    }
    _remove_from_content_pending(index, "123")
    assert "123" not in index["queue"]["content_pending"]
    assert index["repos"]["123"]["content_status"] == "none"

    # 큐에 없어도 content_status만 none으로 마킹
    _remove_from_content_pending(index, "123")
    assert index["repos"]["123"]["content_status"] == "none"

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
@patch("collectors.scripts.prompts.enrich.save_json")
def test_enrich_one_success(mock_save_json: MagicMock, mock_sleep: MagicMock, mock_github_get: MagicMock, tmp_path: Path) -> None:
    index = {"queue": {"content_pending": []}, "repos": {}}

    mock_github_get.side_effect = [
        (200, {"id": 123, "full_name": "owner/repo", "owner": {"type": "User"}, "fork": False, "private": False, "archived": False}, {"ETag": '"etag"'}),
        (200, {"Python": 100}, {})
    ]

    file_entries = [{"file_path": "skill.md"}]

    result = enrich_one("owner/repo", file_entries, index, tmp_path)

    assert result is not None
    assert result["changed"] is True
    assert result["github_id"] == "123"
    assert result["filename"] == "123_owner_repo.json"
    assert "123" in index["queue"]["content_pending"]
    assert index["repos"]["123"]["etag"] == '"etag"'
    mock_save_json.assert_called_once()

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
def test_enrich_one_304(mock_sleep: MagicMock, mock_github_get: MagicMock, tmp_path: Path) -> None:
    index = {
        "queue": {"content_pending": []},
        "repos": {"123": {"source_repo": "owner/repo", "filename": "123_owner_repo.json"}}
    }

    mock_github_get.return_value = (304, None, {})

    result = enrich_one("owner/repo", [], index, tmp_path, stored_etag='"etag"')

    assert result is not None
    assert result["changed"] is False
    assert result["github_id"] == "123"
    assert "123" in index["queue"]["content_pending"]

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
def test_enrich_one_404(mock_sleep: MagicMock, mock_github_get: MagicMock, tmp_path: Path) -> None:
    index = {
        "queue": {"content_pending": ["123"]},
        "repos": {"123": {"source_repo": "owner/repo", "filename": "123_owner_repo.json", "active": True}}
    }

    mock_github_get.return_value = (404, None, {})

    result = enrich_one("owner/repo", [], index, tmp_path)

    assert result is None
    assert index["repos"]["123"]["active"] is False
    assert index["repos"]["123"]["content_status"] == "none"
    assert "123" not in index["queue"]["content_pending"]

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
def test_enrich_one_private(mock_sleep: MagicMock, mock_github_get: MagicMock, tmp_path: Path) -> None:
    """private 레포: active False 마킹 + content_pending에서 제거."""
    index = {
        "queue": {"content_pending": ["123"]},
        "repos": {"123": {"source_repo": "owner/repo", "active": True, "content_status": "pending"}},
    }

    mock_github_get.return_value = (200, {"private": True}, {})

    result = enrich_one("owner/repo", [], index, tmp_path)

    assert result is None
    assert index["repos"]["123"]["active"] is False
    assert index["repos"]["123"]["content_status"] == "none"
    assert "123" not in index["queue"]["content_pending"]

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
def test_enrich_one_private_new_repo(mock_sleep: MagicMock, mock_github_get: MagicMock, tmp_path: Path) -> None:
    """index에 없는 신규 레포가 private인 경우: index 변경 없이 None 반환."""
    index = {
        "queue": {"content_pending": []},
        "repos": {},
    }

    mock_github_get.return_value = (200, {"private": True}, {})

    result = enrich_one("owner/new-repo", [], index, tmp_path)

    assert result is None
    assert index["repos"] == {}
    assert index["queue"]["content_pending"] == []

@patch("collectors.scripts.prompts.enrich.github_get")
@patch("collectors.scripts.prompts.enrich.time.sleep")
def test_enrich_one_fork(mock_sleep: MagicMock, mock_github_get: MagicMock, tmp_path: Path) -> None:
    """fork 레포: None 반환, index 변경 없음."""
    index = {"queue": {"content_pending": []}, "repos": {}}

    mock_github_get.return_value = (200, {"private": False, "fork": True}, {})
    with patch("collectors.scripts.prompts.enrich.SKIP_FORKS", True):
        assert enrich_one("owner/repo", [], index, tmp_path) is None