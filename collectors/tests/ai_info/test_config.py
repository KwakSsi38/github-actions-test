import os
from pathlib import Path

import pytest

from collectors.scripts.ai_info import config


def test_ai_info_config() -> None:
    assert isinstance(config.ROOT_DIR, Path)
    assert isinstance(config.DATA_DIR, Path)
    assert isinstance(config.AI_INFO_DIR, Path)
    assert isinstance(config.OPENROUTER_MODELS_URL, str)
    assert isinstance(config.ARTIFICIAL_ANALYSIS_URL, str)
    assert isinstance(config.REQUEST_TIMEOUT_SECONDS, int)
    assert isinstance(config.MAX_RETRY_ATTEMPTS, int)
    assert isinstance(config.RETRY_WAIT_MIN_SECONDS, int)
    assert isinstance(config.RETRY_WAIT_MAX_SECONDS, int)
    assert isinstance(config.OCI_PREFIX, str)
    assert isinstance(config.OCI_UPLOAD_TARGETS, list)
    for local_path, object_name in config.OCI_UPLOAD_TARGETS:
        assert isinstance(local_path, Path)
        assert isinstance(object_name, str)
        # 로컬 경로와 OCI 경로 모두 ai-info 디렉터리로 통일되어 있어야 한다
        assert str(local_path).find("ai-info") != -1
        assert object_name.startswith(config.OCI_PREFIX)