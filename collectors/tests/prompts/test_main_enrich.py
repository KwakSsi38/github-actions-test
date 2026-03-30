import pytest
from unittest.mock import patch, MagicMock
from pathlib import Path

from collectors.scripts.prompts.main_enrich import _cleanup, _collect_and_enqueue, _restore_file_entries


@patch("collectors.scripts.prompts.main_enrich.WORK_DIR")
@patch("collectors.scripts.prompts.main_enrich.shutil.rmtree")
def test_cleanup(mock_rmtree: MagicMock, mock_work_dir: MagicMock) -> None:
    mock_work_dir.exists.return_value = True
    _cleanup()
    mock_rmtree.assert_called_once_with(mock_work_dir)


@patch("collectors.scripts.prompts.main_enrich.collect_repos")
@patch("collectors.scripts.prompts.main_enrich.notify_start")
def test_collect_and_enqueue_new_repo(mock_notify_start: MagicMock, mock_collect_repos: MagicMock) -> None:
    """신규 레포: enrich_pending 추가 + pending_file_entries에 file_entries 저장."""
    mock_oci = MagicMock()
    mock_collect_repos.return_value = {
        "new/repo": [{"file_path": "skill.md"}],
    }

    index = {
        "queue": {"enrich_pending": []},
        "repos": {},
        "pending_file_entries": {},
        "failed_repos": {},
    }

    new_count, removed_count = _collect_and_enqueue(mock_oci, index)

    assert "new/repo" in index["queue"]["enrich_pending"]
    assert index["pending_file_entries"]["new/repo"] == [{"file_path": "skill.md"}]
    assert "new/repo" not in index["failed_repos"]  # failed_repos에는 저장 안 함
    assert new_count == 1
    assert removed_count == 0
    mock_notify_start.assert_called_once()
    mock_oci.save_index.assert_called_once()


@patch("collectors.scripts.prompts.main_enrich.collect_repos")
@patch("collectors.scripts.prompts.main_enrich.notify_start")
def test_collect_and_enqueue_existing_repo(mock_notify_start: MagicMock, mock_collect_repos: MagicMock) -> None:
    """기존 레포: enrich_pending에만 추가, pending_file_entries에는 저장 안 함."""
    mock_oci = MagicMock()
    mock_collect_repos.return_value = {
        "existing/repo": [{"file_path": "skill.md"}],
    }

    index = {
        "queue": {"enrich_pending": []},
        "repos": {"1": {"source_repo": "existing/repo", "active": True}},
        "pending_file_entries": {},
        "failed_repos": {},
    }

    new_count, removed_count = _collect_and_enqueue(mock_oci, index)

    assert "existing/repo" in index["queue"]["enrich_pending"]
    assert "existing/repo" not in index["pending_file_entries"]  # 기존 레포는 저장 안 함
    assert new_count == 0
    assert removed_count == 0


@patch("collectors.scripts.prompts.main_enrich.collect_repos")
@patch("collectors.scripts.prompts.main_enrich.notify_start")
def test_collect_and_enqueue_removed_repo(mock_notify_start: MagicMock, mock_collect_repos: MagicMock) -> None:
    """Sourcegraph에서 사라진 레포: active False 마킹."""
    mock_oci = MagicMock()
    mock_collect_repos.return_value = {}  # 아무것도 발견 안 됨

    index = {
        "queue": {"enrich_pending": []},
        "repos": {"1": {"source_repo": "gone/repo", "active": True}},
        "pending_file_entries": {},
        "failed_repos": {},
    }

    new_count, removed_count = _collect_and_enqueue(mock_oci, index)

    assert index["repos"]["1"]["active"] is False
    assert new_count == 0
    assert removed_count == 1


@patch("collectors.scripts.prompts.main_enrich.collect_repos")
@patch("collectors.scripts.prompts.main_enrich.notify_start")
def test_collect_and_enqueue_returns_counts(mock_notify_start: MagicMock, mock_collect_repos: MagicMock) -> None:
    """new_count, removed_count 반환값 확인."""
    mock_oci = MagicMock()
    mock_collect_repos.return_value = {
        "new/repo1": [{"file_path": "skill.md"}],
        "new/repo2": [{"file_path": "skill.md"}],
        "existing/repo": [{"file_path": "skill.md"}],
    }

    index = {
        "queue": {"enrich_pending": []},
        "repos": {
            "1": {"source_repo": "existing/repo", "active": True},
            "2": {"source_repo": "gone/repo", "active": True},
        },
        "pending_file_entries": {},
        "failed_repos": {},
    }

    new_count, removed_count = _collect_and_enqueue(mock_oci, index)

    assert new_count == 2
    assert removed_count == 1


@patch("collectors.scripts.prompts.main_enrich.load_json")
@patch("collectors.scripts.prompts.main_enrich.WORK_DIR")
def test_restore_file_entries_from_work_dir(mock_work_dir: MagicMock, mock_load_json: MagicMock, tmp_path: Path) -> None:
    """work_dir에 파일이 있으면 skills에서 file_entries 복원."""
    mock_oci = MagicMock()
    mock_oci.download_file.return_value = True

    local_file = tmp_path / "123_owner_repo.json"
    local_file.write_text("{}")  # 파일 존재만 시뮬레이션

    mock_work_dir.__truediv__ = lambda self, other: local_file  # WORK_DIR / filename
    mock_load_json.return_value = {
        "skills": [
            {"file_path": "path/to/SKILL.md", "content_md": "...", "content_hash": "abc"},
            {"file_path": "other/SKILL.md", "content_md": "...", "content_hash": "def"},
        ]
    }

    index = {
        "repos": {"123": {"filename": "123_owner_repo.json"}},
    }

    result = _restore_file_entries("owner/repo", "123", index, mock_oci)

    assert result == [{"file_path": "path/to/SKILL.md"}, {"file_path": "other/SKILL.md"}]


@patch("collectors.scripts.prompts.main_enrich.WORK_DIR")
def test_restore_file_entries_no_file(mock_work_dir: MagicMock, tmp_path: Path) -> None:
    """work_dir에 파일도 없고 OCI 다운로드도 실패하면 None 반환."""
    mock_oci = MagicMock()
    mock_oci.download_file.return_value = False

    non_existent = tmp_path / "no_such_file.json"
    mock_work_dir.__truediv__ = lambda self, other: non_existent

    index = {
        "repos": {"123": {"filename": "123_owner_repo.json"}},
    }

    result = _restore_file_entries("owner/repo", "123", index, mock_oci)

    assert result is None