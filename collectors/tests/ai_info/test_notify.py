"""
test_notify.py — ai_info notify 단위 테스트

검증 대상:
  - notify_success: send_embed 호출 여부 및 file_count 반영
  - notify_failure: succeeded/failed 없을 때 원본 exc 그대로 전달
  - notify_failure: succeeded만 있을 때 메시지에 포함
  - notify_failure: failed만 있을 때 메시지에 포함
  - notify_failure: succeeded/failed 모두 있을 때 메시지에 모두 포함
"""

from unittest.mock import patch, MagicMock

from collectors.scripts.ai_info.notify import notify_success, notify_failure


@patch("collectors.scripts.ai_info.notify.send_embed")
def test_notify_success_calls_send_embed(mock_send_embed: MagicMock) -> None:
    """notify_success는 file_count를 포함한 embed를 전송해야 한다."""
    notify_success(2)

    mock_send_embed.assert_called_once()
    payload = mock_send_embed.call_args[0][0]
    description = payload["embeds"][0]["description"]
    assert "2" in description


@patch("collectors.scripts.ai_info.notify.send_error")
def test_notify_failure_no_detail(mock_send_error: MagicMock) -> None:
    """succeeded/failed 없으면 원본 exc를 그대로 send_error에 전달해야 한다."""
    exc = RuntimeError("업로드 실패")
    notify_failure("upload_to_oci", exc)

    mock_send_error.assert_called_once()
    forwarded_exc = mock_send_error.call_args[0][1]
    assert forwarded_exc is exc


@patch("collectors.scripts.ai_info.notify.send_error")
def test_notify_failure_with_succeeded_only(mock_send_error: MagicMock) -> None:
    """succeeded만 전달 시 메시지에 성공 내역이 포함되어야 한다."""
    notify_failure(
        "upload_to_oci",
        RuntimeError("실패"),
        succeeded=["data/ai-info/models_info_raw.json"],
    )

    forwarded_exc = mock_send_error.call_args[0][1]
    assert "models_info_raw.json" in str(forwarded_exc)
    assert "✓ 성공" in str(forwarded_exc)


@patch("collectors.scripts.ai_info.notify.send_error")
def test_notify_failure_with_failed_only(mock_send_error: MagicMock) -> None:
    """failed만 전달 시 메시지에 실패 내역이 포함되어야 한다."""
    notify_failure(
        "upload_to_oci",
        RuntimeError("실패"),
        failed=["data/ai-info/models_benchmark_raw.json"],
    )

    forwarded_exc = mock_send_error.call_args[0][1]
    assert "models_benchmark_raw.json" in str(forwarded_exc)
    assert "✗ 실패" in str(forwarded_exc)


@patch("collectors.scripts.ai_info.notify.send_error")
def test_notify_failure_with_both(mock_send_error: MagicMock) -> None:
    """succeeded/failed 모두 전달 시 메시지에 양쪽 내역이 모두 포함되어야 한다."""
    notify_failure(
        "upload_to_oci",
        RuntimeError("부분 실패"),
        succeeded=["data/ai-info/models_info_raw.json"],
        failed=["data/ai-info/models_benchmark_raw.json"],
    )

    forwarded_exc = mock_send_error.call_args[0][1]
    msg = str(forwarded_exc)
    assert "✓ 성공" in msg
    assert "✗ 실패" in msg
    assert "models_info_raw.json" in msg
    assert "models_benchmark_raw.json" in msg
