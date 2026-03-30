import os
from pathlib import Path

import pytest

from collectors.scripts.ai_info import config

def test_ai_info_config() -> None:
    assert isinstance(config.ROOT_DIR, Path)
    assert isinstance(config.DATA_DIR, Path)
    assert isinstance(config.AI_INFO_DIR, Path)
    assert isinstance(config.RANKINGS_DIR, Path)
    assert isinstance(config.OPENROUTER_MODELS_URL, str)
    assert isinstance(config.ARTIFICIAL_ANALYSIS_URL, str)
    assert isinstance(config.REQUEST_TIMEOUT_SECONDS, int)
    assert isinstance(config.MAX_RETRY_ATTEMPTS, int)
    assert isinstance(config.RETRY_WAIT_MIN_SECONDS, int)
    assert isinstance(config.RETRY_WAIT_MAX_SECONDS, int)
    assert isinstance(config.OCI_PREFIX, str)
    assert isinstance(config.OCI_UPLOAD_TARGETS, list)
    for target in config.OCI_UPLOAD_TARGETS:
        assert len(target) == 2
        assert isinstance(target[0], Path)
        assert isinstance(target[1], str)