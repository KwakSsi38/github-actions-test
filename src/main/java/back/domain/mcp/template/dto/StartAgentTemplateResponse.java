package back.domain.mcp.template.dto;

public record StartAgentTemplateResponse(
        String templateName,
        String version,
        String templateMarkdown) {}
