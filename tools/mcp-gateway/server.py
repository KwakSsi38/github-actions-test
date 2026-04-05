from __future__ import annotations

from typing import Any

from mcp.server.fastmcp import FastMCP

from gateway.auto_flow_service import AutoFlowService
from gateway.input_normalizer import GatewayValidationError
from gateway.settings import GatewaySettings
from gateway.spring_proxy_client import (
    GatewayConnectionError,
    GatewayHttpError,
    SpringProxyClient,
)

mcp = FastMCP("start-ai-mcp-gateway")
settings = GatewaySettings.from_env()
client = SpringProxyClient(
    spring_base_url=settings.spring_base_url,
    timeout_seconds=settings.timeout_seconds,
    default_mcp_personal_token=settings.mcp_personal_token,
)
auto_flow_service = AutoFlowService(client)

def _error(error_type: str, message: str, **extra: Any) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "success": False,
        "errorType": error_type,
        "message": message,
    }
    payload.update(extra)
    return payload


def _success(payload: Any) -> dict[str, Any]:
    if isinstance(payload, dict):
        response = dict(payload)
    else:
        response = {"data": payload}

    response["success"] = True
    return response


@mcp.tool(name="get_start_agent_template")
def get_start_agent_template(agentType: str, mcpPersonalToken: str | None = None) -> dict[str, Any]:
    """Fetches start.agent.md template. Uses tool token first, then MCP_PERSONAL_TOKEN env fallback."""
    try:
        response = client.get_start_agent_template(
            mcp_personal_token=mcpPersonalToken,
            agent_type=agentType,
        )
        return _success(response)
    except GatewayValidationError as validation_error:
        return _error("validation_error", str(validation_error))
    except GatewayHttpError as http_error:
        return _error(
            "spring_http_error",
            "Failed to fetch start-agent template from Spring API.",
            statusCode=http_error.status_code,
            responseBody=http_error.response_body,
        )
    except GatewayConnectionError as connection_error:
        return _error("connection_error", str(connection_error))


@mcp.tool(name="recommend_skills")
def recommend_skills(keywords: str, mcpPersonalToken: str | None = None) -> dict[str, Any]:
    """Fetches ranked skills recommendation. Uses tool token first, then MCP_PERSONAL_TOKEN env fallback."""
    try:
        response = client.recommend_skills(
            mcp_personal_token=mcpPersonalToken,
            keywords=keywords,
        )
        return _success(response)
    except GatewayValidationError as validation_error:
        return _error("validation_error", str(validation_error))
    except GatewayHttpError as http_error:
        return _error(
            "spring_http_error",
            "Failed to fetch recommendations from Spring API.",
            statusCode=http_error.status_code,
            responseBody=http_error.response_body,
        )
    except GatewayConnectionError as connection_error:
        return _error("connection_error", str(connection_error))


@mcp.tool(name="start_auto_flow")
def start_auto_flow(
        step: str = "START",
        agentType: str | None = None,
        keywords: str | None = None,
        decision: str | None = None,
        customizationNotes: str | None = None,
        mcpPersonalToken: str | None = None,
) -> dict[str, Any]:
    """
    Runs single-tool auto flow in three steps:
    - START: fetch start.agent template and return write action
    - COLLECTED: fetch recommendations and return skills file write actions
    - FINALIZE: generate final agents.md write action
    Agent type resolution:
    - request agentType if provided
    - otherwise CODEX fallback
    """
    try:
        return auto_flow_service.run(
            step=step,
            mcp_personal_token=mcpPersonalToken,
            agent_type=agentType,
            keywords=keywords,
            decision=decision,
            customization_notes=customizationNotes,
        )
    except GatewayValidationError as validation_error:
        return _error("validation_error", str(validation_error))
    except GatewayHttpError as http_error:
        return _error(
            "spring_http_error",
            "Failed to process start_auto_flow with Spring API.",
            statusCode=http_error.status_code,
            responseBody=http_error.response_body,
        )
    except GatewayConnectionError as connection_error:
        return _error("connection_error", str(connection_error))


if __name__ == "__main__":
    mcp.run(transport="stdio")
