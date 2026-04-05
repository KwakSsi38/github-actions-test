"""
test_upload_to_oci.py — upload_to_oci.main() 단위 테스트

검증 대상:
  - 전체 성공 → notify_success 호출, sys.exit 없음
  - 파일 없음 → failed 처리 후 sys.exit(1)
  - OCI 업로드 실패(etag None) → failed 처리 후 sys.exit(1)
  - 부분 실패 → succeeded/failed 모두 notify_failure에 전달 후 sys.exit(1)
"""

import pytest
from pathlib import Path
from unittest.mock import patch, MagicMock, call


@patch("collectors.scripts.ai_info.upload_to_oci.setup_logging")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_success")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_failure")
@patch("collectors.scripts.ai_info.upload_to_oci.OciManager")
@patch("collectors.scripts.ai_info.upload_to_oci.OCI_UPLOAD_TARGETS")
def test_all_success(
    mock_targets: MagicMock,
    mock_manager_class: MagicMock,
    mock_notify_failure: MagicMock,
    mock_notify_success: MagicMock,
    mock_setup: MagicMock,
    tmp_path: Path,
) -> None:
    """모든 파일 업로드 성공 시 notify_success를 호출하고 종료해야 한다."""
    file_a = tmp_path / "models_info_raw.json"
    file_b = tmp_path / "models_benchmark_raw.json"
    file_a.write_text("{}")
    file_b.write_text("{}")

    mock_targets.__iter__ = MagicMock(return_value=iter([
        (file_a, "data/ai-info/models_info_raw.json"),
        (file_b, "data/ai-info/models_benchmark_raw.json"),
    ]))
    mock_targets.__len__ = MagicMock(return_value=2)

    mock_manager = MagicMock()
    mock_manager.upload_file.return_value = "etag-abc"
    mock_manager_class.return_value = mock_manager

    from collectors.scripts.ai_info.upload_to_oci import main
    main()

    mock_notify_success.assert_called_once_with(2)
    mock_notify_failure.assert_not_called()


@patch("collectors.scripts.ai_info.upload_to_oci.setup_logging")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_success")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_failure")
@patch("collectors.scripts.ai_info.upload_to_oci.OciManager")
@patch("collectors.scripts.ai_info.upload_to_oci.OCI_UPLOAD_TARGETS")
def test_file_not_exists_exits(
    mock_targets: MagicMock,
    mock_manager_class: MagicMock,
    mock_notify_failure: MagicMock,
    mock_notify_success: MagicMock,
    mock_setup: MagicMock,
    tmp_path: Path,
) -> None:
    """로컬 파일이 없으면 failed 처리 후 sys.exit(1)해야 한다."""
    missing = tmp_path / "not_existing.json"
    object_name = "data/ai-info/models_info_raw.json"

    mock_targets.__iter__ = MagicMock(return_value=iter([(missing, object_name)]))
    mock_targets.__len__ = MagicMock(return_value=1)

    from collectors.scripts.ai_info.upload_to_oci import main
    with pytest.raises(SystemExit):
        main()

    mock_notify_failure.assert_called_once()
    failed_arg = mock_notify_failure.call_args.kwargs["failed"]
    assert object_name in failed_arg
    mock_notify_success.assert_not_called()


@patch("collectors.scripts.ai_info.upload_to_oci.setup_logging")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_success")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_failure")
@patch("collectors.scripts.ai_info.upload_to_oci.OciManager")
@patch("collectors.scripts.ai_info.upload_to_oci.OCI_UPLOAD_TARGETS")
def test_upload_failure_exits(
    mock_targets: MagicMock,
    mock_manager_class: MagicMock,
    mock_notify_failure: MagicMock,
    mock_notify_success: MagicMock,
    mock_setup: MagicMock,
    tmp_path: Path,
) -> None:
    """OCI 업로드 실패(etag None) 시 sys.exit(1)해야 한다."""
    file_a = tmp_path / "models_info_raw.json"
    file_a.write_text("{}")
    object_name = "data/ai-info/models_info_raw.json"

    mock_targets.__iter__ = MagicMock(return_value=iter([(file_a, object_name)]))
    mock_targets.__len__ = MagicMock(return_value=1)

    mock_manager = MagicMock()
    mock_manager.upload_file.return_value = None  # 업로드 실패
    mock_manager_class.return_value = mock_manager

    from collectors.scripts.ai_info.upload_to_oci import main
    with pytest.raises(SystemExit):
        main()

    mock_notify_failure.assert_called_once()
    mock_notify_success.assert_not_called()


@patch("collectors.scripts.ai_info.upload_to_oci.setup_logging")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_success")
@patch("collectors.scripts.ai_info.upload_to_oci.notify_failure")
@patch("collectors.scripts.ai_info.upload_to_oci.OciManager")
@patch("collectors.scripts.ai_info.upload_to_oci.OCI_UPLOAD_TARGETS")
def test_partial_failure_passes_succeeded_and_failed(
    mock_targets: MagicMock,
    mock_manager_class: MagicMock,
    mock_notify_failure: MagicMock,
    mock_notify_success: MagicMock,
    mock_setup: MagicMock,
    tmp_path: Path,
) -> None:
    """부분 실패 시 notify_failure에 succeeded/failed 목록이 모두 전달되어야 한다."""
    file_a = tmp_path / "models_info_raw.json"
    file_b = tmp_path / "models_benchmark_raw.json"
    file_a.write_text("{}")
    file_b.write_text("{}")

    name_a = "data/ai-info/models_info_raw.json"
    name_b = "data/ai-info/models_benchmark_raw.json"

    mock_targets.__iter__ = MagicMock(return_value=iter([
        (file_a, name_a),
        (file_b, name_b),
    ]))
    mock_targets.__len__ = MagicMock(return_value=2)

    mock_manager = MagicMock()
    # file_a 성공, file_b 실패
    mock_manager.upload_file.side_effect = ["etag-abc", None]
    mock_manager_class.return_value = mock_manager

    from collectors.scripts.ai_info.upload_to_oci import main
    with pytest.raises(SystemExit):
        main()

    mock_notify_failure.assert_called_once()
    kwargs = mock_notify_failure.call_args.kwargs
    assert name_a in kwargs["succeeded"]
    assert name_b in kwargs["failed"]
    mock_notify_success.assert_not_called()
