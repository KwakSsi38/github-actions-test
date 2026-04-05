from __future__ import annotations

import json
from typing import Any
from urllib import error, request

from gateway.input_normalizer import (
    GatewayValidationError,
    normalize_agent_type,
    normalize_keywords,
    normalize_mcp_personal_token,
)


class GatewayHttpError(RuntimeError):
    def __init__(self, status_code: int, response_body: str):
        super().__init__(f"Spring API HTTP error: {status_code}")
        self.status_code = status_code
        self.response_body = response_body


class GatewayConnectionError(RuntimeError):
    pass


class SpringProxyClient:
    def __init__(
            self,
            spring_base_url: str,
            timeout_seconds: float = 10.0,
            default_mcp_personal_token: str | None = None,
    ):
        self.spring_base_url = spring_base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds
        self.default_mcp_personal_token = default_mcp_personal_token

    def get_start_agent_template(self, mcp_personal_token: str | None, agent_type: str) -> dict[str, Any]:
        token = self._resolve_mcp_personal_token(mcp_personal_token)
        normalized_agent_type = normalize_agent_type(agent_type)

        return self._post_json(
            path="/api/v1/mcp/template/start-agent",
            bearer_token=token,
            payload={"agentType": normalized_agent_type},
        )

    def recommend_skills(self, mcp_personal_token: str | None, keywords: str) -> dict[str, Any]:
        token = self._resolve_mcp_personal_token(mcp_personal_token)
        normalized_keywords = normalize_keywords(keywords)

        return self._post_json(
            path="/api/v1/mcp/recommendations",
            bearer_token=token,
            payload={"keywords": normalized_keywords},
        )

    def _post_json(self, path: str, bearer_token: str, payload: dict[str, Any]) -> dict[str, Any]:
        url = f"{self.spring_base_url}{path}"
        body = json.dumps(payload).encode("utf-8")

        req = request.Request(
            url=url,
            method="POST",
            data=body,
            headers={
                "Authorization": f"Bearer {bearer_token}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
        )

        try:
            with request.urlopen(req, timeout=self.timeout_seconds) as response:
                response_body = response.read().decode("utf-8")
                if not response_body:
                    return {}

                parsed = json.loads(response_body)
                if not isinstance(parsed, dict):
                    raise GatewayValidationError("Spring API response must be a JSON object.")

                return parsed
        except error.HTTPError as http_error:
            response_body = http_error.read().decode("utf-8", errors="replace")
            raise GatewayHttpError(http_error.code, response_body) from http_error
        except error.URLError as url_error:
            raise GatewayConnectionError(f"Spring API connection failed: {url_error.reason}") from url_error

    def _resolve_mcp_personal_token(self, request_token: str | None) -> str:
        if request_token is not None and request_token.strip():
            return normalize_mcp_personal_token(request_token)

        if self.default_mcp_personal_token is not None and self.default_mcp_personal_token.strip():
            return normalize_mcp_personal_token(self.default_mcp_personal_token)

        raise GatewayValidationError(
            "mcpPersonalToken is required. Set tool argument or MCP_PERSONAL_TOKEN environment variable."
        )
