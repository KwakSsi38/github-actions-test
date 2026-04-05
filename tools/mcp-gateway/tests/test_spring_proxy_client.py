import io
import json
import unittest
from unittest import mock
from urllib import error

from gateway.spring_proxy_client import (
    GatewayConnectionError,
    GatewayHttpError,
    GatewayValidationError,
    SpringProxyClient,
)


class _FakeResponse:
    def __init__(self, body: dict):
        self._bytes = json.dumps(body).encode("utf-8")

    def read(self):
        return self._bytes

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False


class SpringProxyClientTest(unittest.TestCase):
    def setUp(self):
        self.client = SpringProxyClient("http://localhost:8080", timeout_seconds=3)

    @mock.patch("gateway.spring_proxy_client.request.urlopen")
    def test_recommend_skills_sends_authorized_request(self, mock_urlopen):
        mock_urlopen.return_value = _FakeResponse({"message": "추천 성공"})

        response = self.client.recommend_skills("mcp_token_1", " SpringBoot   infra ")

        self.assertEqual(response["message"], "추천 성공")
        request_arg = mock_urlopen.call_args[0][0]
        self.assertEqual(request_arg.full_url, "http://localhost:8080/api/v1/mcp/recommendations")
        self.assertEqual(request_arg.get_header("Authorization"), "Bearer mcp_token_1")

        body = json.loads(request_arg.data.decode("utf-8"))
        self.assertEqual(body["keywords"], "SpringBoot infra")

    @mock.patch("gateway.spring_proxy_client.request.urlopen")
    def test_get_template_sends_expected_payload(self, mock_urlopen):
        mock_urlopen.return_value = _FakeResponse({"message": "템플릿 조회 성공"})

        response = self.client.get_start_agent_template("mcp_token_2", "codex")

        self.assertEqual(response["message"], "템플릿 조회 성공")
        request_arg = mock_urlopen.call_args[0][0]
        self.assertEqual(request_arg.full_url, "http://localhost:8080/api/v1/mcp/template/start-agent")

        body = json.loads(request_arg.data.decode("utf-8"))
        self.assertEqual(body["agentType"], "CODEX")

    @mock.patch("gateway.spring_proxy_client.request.urlopen")
    def test_raises_gateway_http_error(self, mock_urlopen):
        http_error = error.HTTPError(
            url="http://localhost:8080/api/v1/mcp/recommendations",
            code=401,
            msg="Unauthorized",
            hdrs=None,
            fp=io.BytesIO('{"message":"MCP 토큰 인증 실패"}'.encode("utf-8")),
        )
        mock_urlopen.side_effect = http_error

        with self.assertRaises(GatewayHttpError) as ctx:
            self.client.recommend_skills("invalid", "SpringBoot")

        self.assertEqual(ctx.exception.status_code, 401)
        self.assertIn("MCP 토큰 인증 실패", ctx.exception.response_body)

    @mock.patch("gateway.spring_proxy_client.request.urlopen")
    def test_raises_gateway_connection_error(self, mock_urlopen):
        mock_urlopen.side_effect = error.URLError("connection refused")

        with self.assertRaises(GatewayConnectionError):
            self.client.recommend_skills("mcp_token_3", "SpringBoot")

    @mock.patch("gateway.spring_proxy_client.request.urlopen")
    def test_uses_default_token_when_request_token_missing(self, mock_urlopen):
        mock_urlopen.return_value = _FakeResponse({"message": "추천 성공"})
        client = SpringProxyClient(
            "http://localhost:8080",
            timeout_seconds=3,
            default_mcp_personal_token="env_token_1",
        )

        client.recommend_skills(None, "SpringBoot infra")

        request_arg = mock_urlopen.call_args[0][0]
        self.assertEqual(request_arg.get_header("Authorization"), "Bearer env_token_1")

    @mock.patch("gateway.spring_proxy_client.request.urlopen")
    def test_request_token_has_priority_over_default_token(self, mock_urlopen):
        mock_urlopen.return_value = _FakeResponse({"message": "추천 성공"})
        client = SpringProxyClient(
            "http://localhost:8080",
            timeout_seconds=3,
            default_mcp_personal_token="env_token_2",
        )

        client.recommend_skills("request_token_1", "SpringBoot infra")

        request_arg = mock_urlopen.call_args[0][0]
        self.assertEqual(request_arg.get_header("Authorization"), "Bearer request_token_1")

    def test_raises_validation_error_when_both_tokens_missing(self):
        with self.assertRaises(GatewayValidationError):
            self.client.recommend_skills(None, "SpringBoot infra")


if __name__ == "__main__":
    unittest.main()
