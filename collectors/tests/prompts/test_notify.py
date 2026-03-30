from unittest.mock import patch, MagicMock

import pytest

from collectors.scripts.prompts.notify import (
    notify_start,
    notify_complete,
    notify_fail_warning,
    notify_error,
)


@patch("collectors.scripts.prompts.notify.send_embed")
def test_notify_start(mock_send_embed: MagicMock) -> None:
    notify_start(10, 20, 5)
    mock_send_embed.assert_called_once()
    
    embed = mock_send_embed.call_args[0][0]["embeds"][0]
    assert "SKILL.md 수집 시작" in embed["title"]
    assert len(embed["fields"]) == 3
    assert embed["fields"][0]["value"] == "10"
    assert embed["fields"][1]["value"] == "20"
    assert embed["fields"][2]["value"] == "5"


@patch("collectors.scripts.prompts.notify.send_embed")
def test_notify_complete(mock_send_embed: MagicMock) -> None:
    stats = {"new": 10, "updated": 5, "skipped": 2, "failed": 1, "removed": 0}
    notify_complete(stats, 125.0, interrupted=False)
    mock_send_embed.assert_called_once()
    
    embed = mock_send_embed.call_args[0][0]["embeds"][0]
    assert "수집 완료" in embed["title"]
    assert len(embed["fields"]) == 6
    assert embed["fields"][0]["value"] == "10"
    assert embed["fields"][5]["value"] == "2분 5초"


@patch("collectors.scripts.prompts.notify.send_embed")
def test_notify_complete_interrupted(mock_send_embed: MagicMock) -> None:
    stats = {"new": 10, "updated": 5}
    notify_complete(stats, 125.0, interrupted=True)
    mock_send_embed.assert_called_once()
    
    embed = mock_send_embed.call_args[0][0]["embeds"][0]
    assert "수집 중단" in embed["title"]


@patch("collectors.scripts.prompts.notify.send_embed")
@patch("collectors.scripts.prompts.notify.FAIL_ALERT_THRESHOLD", 50)
def test_notify_fail_warning(mock_send_embed: MagicMock) -> None:
    # Less than threshold
    notify_fail_warning(10, 100)
    mock_send_embed.assert_not_called()

    # Greater than threshold
    notify_fail_warning(60, 100)
    mock_send_embed.assert_called_once()
    
    embed = mock_send_embed.call_args[0][0]["embeds"][0]
    assert "실패 임계치 초과" in embed["title"]
    assert "60개" in embed["description"]


@patch("collectors.scripts.prompts.notify.send_error")
def test_notify_error(mock_send_error: MagicMock) -> None:
    exc = ValueError("Test error")
    notify_error(exc)
    mock_send_error.assert_called_once()