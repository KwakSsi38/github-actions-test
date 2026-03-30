import os
from unittest.mock import patch, MagicMock

import pytest

from collectors.scripts.shared.oci_client import BaseOciClient

@pytest.fixture
def mock_oci_client():
    with patch("collectors.scripts.shared.oci_client.oci.config.from_file") as mock_from_file:
        with patch("collectors.scripts.shared.oci_client.oci.object_storage.ObjectStorageClient") as mock_client:
            mock_client_instance = mock_client.return_value
            yield mock_client_instance

def test_put_object_success(mock_oci_client) -> None:
    client = BaseOciClient()
    mock_response = MagicMock()
    mock_response.headers = {"ETag": '"test-etag"'}
    mock_oci_client.put_object.return_value = mock_response

    etag = client.put_object("test_object", b"test_content")
    assert etag == "test-etag"
    mock_oci_client.put_object.assert_called_once()

def test_put_object_failure(mock_oci_client) -> None:
    from oci.exceptions import ServiceError
    client = BaseOciClient()
    mock_oci_client.put_object.side_effect = ServiceError(status=500, code="InternalServerError", message="Error", headers={})

    with patch("collectors.scripts.shared.oci_client.time.sleep"):
        etag = client.put_object("test_object", b"test_content")
    
    assert etag is None
    assert mock_oci_client.put_object.call_count == 3  # Based on UPLOAD_RETRIES = 3 in config

def test_get_object_bytes_success(mock_oci_client) -> None:
    client = BaseOciClient()
    mock_response = MagicMock()
    mock_response.data.content = b"test_content"
    mock_oci_client.get_object.return_value = mock_response

    content = client.get_object_bytes("test_object")
    assert content == b"test_content"
    mock_oci_client.get_object.assert_called_once()

def test_get_object_bytes_not_found(mock_oci_client) -> None:
    from oci.exceptions import ServiceError
    client = BaseOciClient()
    mock_oci_client.get_object.side_effect = ServiceError(status=404, code="NotFound", message="Not Found", headers={})

    content = client.get_object_bytes("test_object")
    assert content is None

def test_delete_object_success(mock_oci_client) -> None:
    client = BaseOciClient()
    client.delete_object("test_object")
    mock_oci_client.delete_object.assert_called_once()

def test_delete_object_not_found_ignored(mock_oci_client) -> None:
    from oci.exceptions import ServiceError
    client = BaseOciClient()
    mock_oci_client.delete_object.side_effect = ServiceError(status=404, code="NotFound", message="Not Found", headers={})

    # Should not raise exception
    client.delete_object("test_object")
    mock_oci_client.delete_object.assert_called_once()

def test_delete_object_other_error(mock_oci_client) -> None:
    from oci.exceptions import ServiceError
    client = BaseOciClient()
    mock_oci_client.delete_object.side_effect = ServiceError(status=500, code="Error", message="Error", headers={})

    with pytest.raises(ServiceError):
        client.delete_object("test_object")