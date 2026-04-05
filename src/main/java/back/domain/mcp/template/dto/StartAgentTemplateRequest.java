package back.domain.mcp.template.dto;

import jakarta.validation.constraints.NotNull;

public record StartAgentTemplateRequest(
        @NotNull(message = "agentType-NotNull-agentType은 필수입니다.")
        AgentType agentType) {}
