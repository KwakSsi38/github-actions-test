import json
import tempfile
from pathlib import Path

import pytest

from collectors.scripts.shared.utils import (
    save_json,
    load_json,
    save_json_str,
    now_iso,
    now_str,
    now_display,
)


def test_save_and_load_json() -> None:
    data = {"key": "value", "list": [1, 2, 3]}
    with tempfile.TemporaryDirectory() as tmp_dir:
        file_path = Path(tmp_dir) / "test.json"
        
        # Save
        save_json(data, file_path)
        assert file_path.exists()
        
        # Load
        loaded_data = load_json(file_path)
        assert data == loaded_data


def test_load_json_file_not_found() -> None:
    with tempfile.TemporaryDirectory() as tmp_dir:
        file_path = Path(tmp_dir) / "not_found.json"
        with pytest.raises(FileNotFoundError):
            load_json(file_path)


def test_save_json_str() -> None:
    data = {"key": "value"}
    json_str = save_json_str(data)
    assert isinstance(json_str, str)
    assert json.loads(json_str) == data


def test_now_iso() -> None:
    result = now_iso()
    assert isinstance(result, str)
    assert "T" in result
    assert result.endswith("+00:00")


def test_now_str() -> None:
    result = now_str()
    assert isinstance(result, str)
    assert len(result) == 19
    assert "-" in result and ":" in result


def test_now_display() -> None:
    result = now_display()
    assert isinstance(result, str)
    assert result.endswith(" UTC")