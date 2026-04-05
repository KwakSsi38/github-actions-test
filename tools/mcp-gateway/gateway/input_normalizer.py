from __future__ import annotations

SUPPORTED_AGENT_TYPES = {"CLAUDE", "CODEX", "GEMINI"}
SUPPORTED_FLOW_STEPS = {"START", "COLLECTED", "FINALIZE"}
SUPPORTED_FINALIZE_DECISIONS = {"ACCEPT", "CUSTOMIZE"}


class GatewayValidationError(ValueError):
    """Raised when tool input is invalid."""


def normalize_mcp_personal_token(token: str) -> str:
    if token is None:
        raise GatewayValidationError("mcpPersonalToken is required.")

    normalized = token.strip()
    if not normalized:
        raise GatewayValidationError("mcpPersonalToken must not be blank.")

    return normalized


def normalize_keywords(keywords: str) -> str:
    if keywords is None:
        raise GatewayValidationError("keywords is required.")

    normalized = " ".join(keywords.split())
    if not normalized:
        raise GatewayValidationError("keywords must not be blank.")

    return normalized


def normalize_agent_type(agent_type: str) -> str:
    if agent_type is None:
        raise GatewayValidationError("agentType is required.")

    normalized = agent_type.strip().upper()
    if normalized not in SUPPORTED_AGENT_TYPES:
        raise GatewayValidationError(
            "agentType must be one of: CLAUDE, CODEX, GEMINI."
        )

    return normalized


def normalize_flow_step(step: str) -> str:
    if step is None:
        raise GatewayValidationError("step is required.")

    normalized = step.strip().upper()
    if normalized not in SUPPORTED_FLOW_STEPS:
        raise GatewayValidationError(
            "step must be one of: START, COLLECTED, FINALIZE."
        )

    return normalized


def normalize_finalize_decision(decision: str) -> str:
    if decision is None:
        raise GatewayValidationError("decision is required for FINALIZE step.")

    normalized = decision.strip().upper()
    if normalized not in SUPPORTED_FINALIZE_DECISIONS:
        raise GatewayValidationError(
            "decision must be one of: ACCEPT, CUSTOMIZE."
        )

    return normalized
