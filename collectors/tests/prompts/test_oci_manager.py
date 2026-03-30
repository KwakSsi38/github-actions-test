import json
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from collectors.scripts.prompts.oci_manager import OciManager


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_oci_manager_init(mock_init: MagicMock) -> None:
    manager = OciManager()
    assert manager is not None


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_acquire_lock_success(mock_init: MagicMock) -> None:
    manager = OciManager()
    manager.get_object_bytes = MagicMock(return_value=None)
    manager.put_object = MagicMock()
    
    assert manager.acquire_lock() is True
    manager.put_object.assert_called_once()


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
@patch("collectors.scripts.prompts.oci_manager.time.time", return_value=1000)
def test_acquire_lock_fail(mock_time: MagicMock, mock_init: MagicMock) -> None:
    manager = OciManager()
    lock_data = json.dumps({"locked_at": 900}).encode()  # Within LOCK_TTL_SEC
    manager.get_object_bytes = MagicMock(return_value=lock_data)
    manager.put_object = MagicMock()
    
    assert manager.acquire_lock() is False
    manager.put_object.assert_not_called()


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_release_lock(mock_init: MagicMock) -> None:
    manager = OciManager()
    manager.delete_object = MagicMock()
    
    manager.release_lock()
    manager.delete_object.assert_called_once()


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_load_index_new(mock_init: MagicMock) -> None:
    manager = OciManager()
    manager.get_object_bytes = MagicMock(return_value=None)
    
    index = manager.load_index()
    assert isinstance(index, dict)
    assert "queue" in index


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_load_index_existing(mock_init: MagicMock) -> None:
    manager = OciManager()
    existing_index = {"queue": {"enrich_pending": []}}
    manager.get_object_bytes = MagicMock(return_value=json.dumps(existing_index).encode())
    
    index = manager.load_index()
    assert index == existing_index


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_upload_file(mock_init: MagicMock, tmp_path: Path) -> None:
    manager = OciManager()
    manager.put_object = MagicMock(return_value="test-etag")
    
    test_file = tmp_path / "test.json"
    test_file.write_text('{"test": "data"}')
    
    etag = manager.upload_file("test.json", tmp_path)
    assert etag == "test-etag"
    manager.put_object.assert_called_once()


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_download_file(mock_init: MagicMock, tmp_path: Path) -> None:
    manager = OciManager()
    manager.get_object_bytes = MagicMock(return_value=b'{"test": "data"}')
    
    success = manager.download_file("test.json", tmp_path)
    assert success is True
    assert (tmp_path / "test.json").exists()


@patch("collectors.scripts.prompts.oci_manager.BaseOciClient.__init__", return_value=None)
def test_save_index(mock_init: MagicMock) -> None:
    manager = OciManager()
    manager.put_object = MagicMock(return_value="test-etag")
    
    index = {"queue": {"enrich_pending": [], "content_pending": []}}
    stats = {"new": 1}
    
    manager.save_index(index, stats, 10.0)
    manager.put_object.assert_called_once()
    assert "last_updated" in index
    assert "run_history" in index
    assert index["stats"] == stats