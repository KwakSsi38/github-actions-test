import pytest
from unittest.mock import patch, MagicMock
from pathlib import Path

from collectors.scripts.prompts.main_enrich import _cleanup, _collect_and_enqueue

@patch("collectors.scripts.prompts.main_enrich.WORK_DIR")
@patch("collectors.scripts.prompts.main_enrich.shutil.rmtree")
def test_cleanup(mock_rmtree: MagicMock, mock_work_dir: MagicMock) -> None:
    mock_work_dir.exists.return_value = True
    _cleanup()
    mock_rmtree.assert_called_once_with(mock_work_dir)

@patch("collectors.scripts.prompts.main_enrich.collect_repos")
@patch("collectors.scripts.prompts.main_enrich.notify_start")
def test_collect_and_enqueue(mock_notify_start: MagicMock, mock_collect_repos: MagicMock) -> None:
    mock_oci = MagicMock()
    mock_collect_repos.return_value = {
        "new/repo": [{"file_path": "skill.md"}],
        "existing/repo": [{"file_path": "skill.md"}]
    }
    
    index = {
        "queue": {"enrich_pending": []},
        "repos": {"1": {"source_repo": "existing/repo", "active": True}},
        "failed_repos": {}
    }
    
    _collect_and_enqueue(mock_oci, index)
    
    # "new/repo" and "existing/repo" should be added to enrich_pending
    assert "new/repo" in index["queue"]["enrich_pending"]
    assert "existing/repo" in index["queue"]["enrich_pending"]
    assert "new/repo" in index["failed_repos"]
    
    mock_notify_start.assert_called_once()
    mock_oci.save_index.assert_called_once()