import pytest
from unittest.mock import patch, MagicMock
from pathlib import Path

from collectors.scripts.prompts.main_contents import _cleanup, _download_pending

@patch("collectors.scripts.prompts.main_contents.WORK_DIR")
@patch("collectors.scripts.prompts.main_contents.shutil.rmtree")
def test_cleanup(mock_rmtree: MagicMock, mock_work_dir: MagicMock) -> None:
    mock_work_dir.exists.return_value = True
    _cleanup()
    mock_rmtree.assert_called_once_with(mock_work_dir)

@patch("collectors.scripts.prompts.main_contents.WORK_DIR")
def test_download_pending(mock_work_dir: MagicMock) -> None:
    mock_oci = MagicMock()
    mock_oci.download_file.return_value = True
    
    index = {
        "repos": {
            "123": {"filename": "123_repo.json"},
            "456": {"filename": "456_repo.json"}
        }
    }
    pending = ["123", "456", "789"] # 789 is missing in index
    
    _download_pending(mock_oci, index, pending)
    
    assert mock_oci.download_file.call_count == 2
    mock_oci.download_file.assert_any_call("123_repo.json", mock_work_dir)
    mock_oci.download_file.assert_any_call("456_repo.json", mock_work_dir)