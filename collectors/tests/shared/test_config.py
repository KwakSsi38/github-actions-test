import os

import pytest

from collectors.scripts.shared import config


def test_shared_config() -> None:
    assert isinstance(config.UPLOAD_RETRIES, int)
    assert isinstance(config.UPLOAD_DELAY, float)
    assert isinstance(config.REQUEST_TIMEOUT_SECONDS, int)
    assert isinstance(config.MAX_RETRY_ATTEMPTS, int)
    assert isinstance(config.RETRY_WAIT_MIN_SECONDS, int)
    assert isinstance(config.RETRY_WAIT_MAX_SECONDS, int)
    assert isinstance(config.LOG_FORMAT, str)
    assert isinstance(config.LOG_DATE_FORMAT, str)