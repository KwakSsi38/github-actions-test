import os
from pathlib import Path

import pytest

from collectors.scripts.prompts import config

def test_prompts_config() -> None:
    assert isinstance(config.OCI_DATA_PREFIX, str)
    assert isinstance(config.OCI_INDEX_OBJECT, str)
    assert isinstance(config.OCI_LOCK_OBJECT, str)
    assert isinstance(config.LOCK_TTL_SEC, int)
    assert isinstance(config.CHECKPOINT_N, int)
    assert isinstance(config.TIMEOUT_MARGIN, int)
    assert isinstance(config.MAX_RUNTIME_SEC, int)
    assert isinstance(config.WORK_DIR, Path)
    assert isinstance(config.SOURCEGRAPH_TOKEN, str)
    assert isinstance(config.SOURCEGRAPH_URL, str)
    assert isinstance(config.SOURCEGRAPH_QUERY, str)
    assert isinstance(config.SKIP_FORKS, bool)
    assert isinstance(config.SIZE_LIMIT, int)
    assert isinstance(config.MIN_CONTENT_LEN, int)
    assert isinstance(config.FAIL_ALERT_THRESHOLD, int)
    assert isinstance(config.SENSITIVE_VALUES, list)