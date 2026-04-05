package back.domain.mcp.template.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import back.domain.mcp.template.dto.AgentType;
import back.domain.mcp.template.dto.StartAgentTemplateResponse;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StartAgentTemplateServiceImpl implements StartAgentTemplateService {
    private static final String TEMPLATE_NAME = "start.agent.md";
    private static final String TEMPLATE_VERSION = "v4";
    private static final String TEMPLATE_READ_FAILED_MESSAGE = "템플릿 조회 중 오류가 발생했습니다.";
    private static final String TEMPLATE_PATH = "templates/mcp/start-agent/start.agent.template.md";
    private static final String AGENT_TYPE_PLACEHOLDER = "{AGENT_TYPE}";

    private final ResourceLoader resourceLoader;

    @Override
    public StartAgentTemplateResponse getTemplate(AgentType agentType) {
        String templateMarkdown = readTemplateMarkdown()
                .replace(AGENT_TYPE_PLACEHOLDER, agentType.name());
        return new StartAgentTemplateResponse(TEMPLATE_NAME, TEMPLATE_VERSION, templateMarkdown);
    }

    private String readTemplateMarkdown() {
        Resource resource = resourceLoader.getResource("classpath:" + TEMPLATE_PATH);
        if (!resource.exists()) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[StartAgentTemplateServiceImpl#readTemplateMarkdown] "
                            + "template resource is missing: " + TEMPLATE_PATH,
                    TEMPLATE_READ_FAILED_MESSAGE);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[StartAgentTemplateServiceImpl#readTemplateMarkdown] "
                            + "template resource read failed: " + TEMPLATE_PATH
                            + " (cause: " + exception.getClass().getSimpleName()
                            + ": " + exception.getMessage() + ")",
                    TEMPLATE_READ_FAILED_MESSAGE);
        }
    }
}
