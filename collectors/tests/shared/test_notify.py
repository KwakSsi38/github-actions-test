import os
from unittest.mock import patch, MagicMock

import httpx
import pytest

from collectors.scripts.shared.notify import (
    mask_sensitive,
    send_embed,
    send_error,
)


def test_mask_sensitive() -> None:
    text = "secret_key=123456"
    masked = mask_sensitive(text, ["123456"])
    assert masked == "secret_key=***MASKED***"


@patch("collectors.scripts.shared.notify.DISCORD_WEBHOOK_URL", "http://test-webhook")
@patch("httpx.Client.post")
def test_send_embed_success(mock_post: MagicMock) -> None:
    mock_post.return_value.status_code = 204
    send_embed({"test": "data"})
    mock_post.assert_called_once_with("http://test-webhook", json={"test": "data"})


@patch("collectors.scripts.shared.notify.DISCORD_WEBHOOK_URL", "")
@patch("httpx.Client.post")
def test_send_embed_no_webhook(mock_post: MagicMock) -> None:
    send_embed({"test": "data"})
    mock_post.assert_not_called()


@patch("collectors.scripts.shared.notify.send_embed")
def test_send_error(mock_send_embed: MagicMock) -> None:
    try:
        raise ValueError("test error")
    except ValueError as e:
        send_error("Test Error", e)

    mock_send_embed.assert_called_once()
    embeds = mock_send_embed.call_args[0][0]["embeds"]
    assert len(embeds) == 1
    assert "Test Error" in embeds[0]["title"]
    assert "ValueError: test error" in embeds[0]["description"]