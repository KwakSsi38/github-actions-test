import os
import unittest
from unittest.mock import patch

from gateway.settings import GatewaySettings


class GatewaySettingsTest(unittest.TestCase):
    def test_from_env_uses_defaults_when_not_set(self):
        with patch.dict(os.environ, {}, clear=True):
            settings = GatewaySettings.from_env()

        self.assertEqual(settings.spring_base_url, "http://localhost:8080")
        self.assertEqual(settings.timeout_seconds, 10.0)
        self.assertIsNone(settings.mcp_personal_token)

    def test_from_env_raises_clear_error_when_timeout_is_invalid(self):
        with patch.dict(os.environ, {"GATEWAY_HTTP_TIMEOUT_SECONDS": "abc"}, clear=True):
            with self.assertRaisesRegex(
                    ValueError,
                    "Invalid value for GATEWAY_HTTP_TIMEOUT_SECONDS"
            ):
                GatewaySettings.from_env()


if __name__ == "__main__":
    unittest.main()
