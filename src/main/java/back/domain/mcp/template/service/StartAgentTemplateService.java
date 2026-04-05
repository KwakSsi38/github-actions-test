package back.domain.mcp.template.service;

import back.domain.mcp.template.dto.AgentType;
import back.domain.mcp.template.dto.StartAgentTemplateResponse;

public interface StartAgentTemplateService {
    StartAgentTemplateResponse getTemplate(AgentType agentType);
}
