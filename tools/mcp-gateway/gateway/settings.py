from dataclasses import dataclass
import os


@dataclass(frozen=True)
class GatewaySettings:
    spring_base_url: str
    timeout_seconds: float
    mcp_personal_token: str | None

    @staticmethod
    def from_env() -> "GatewaySettings":
        base_url = os.getenv("SPRING_API_BASE_URL", "http://localhost:8080").rstrip("/")
        raw_timeout_seconds = os.getenv("GATEWAY_HTTP_TIMEOUT_SECONDS", "10")
        try:
            timeout_seconds = float(raw_timeout_seconds)
        except ValueError as exc:
            raise ValueError(
                "Invalid value for GATEWAY_HTTP_TIMEOUT_SECONDS: "
                f"{raw_timeout_seconds!r}. Expected a numeric value."
            ) from exc
        raw_token = os.getenv("MCP_PERSONAL_TOKEN")
        mcp_personal_token = raw_token.strip() if raw_token and raw_token.strip() else None

        return GatewaySettings(
            spring_base_url=base_url,
            timeout_seconds=timeout_seconds,
            mcp_personal_token=mcp_personal_token,
        )
