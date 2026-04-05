package back.domain.mcp.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import back.domain.mcp.template.dto.AgentType;
import back.domain.mcp.template.dto.StartAgentTemplateResponse;

class StartAgentTemplateServiceTest {

    private final StartAgentTemplateService startAgentTemplateService =
            new StartAgentTemplateServiceImpl(new DefaultResourceLoader());

    @Test
    @DisplayName("CLAUDE 템플릿을 조회할 수 있다")
    void getTemplate_claude() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.CLAUDE);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v4");
        assertThat(response.templateMarkdown()).contains("CLAUDE");
        assertThat(response.templateMarkdown()).contains("start_auto_flow");
        assertThat(response.templateMarkdown()).contains("자동화 워크 플로우 진행해줘");
        assertThat(response.templateMarkdown()).contains("actions.writeFiles");
        assertThat(response.templateMarkdown()).contains("부분 보정");
        assertThat(response.templateMarkdown()).contains("새 문서를 처음부터 다시 작성하지 말고");
        assertThat(response.templateMarkdown()).contains("추천 API를 다시 호출하지 않고, 이미 생성된 skills 파일 기준으로 최종화");
    }

    @Test
    @DisplayName("CODEX 템플릿을 조회할 수 있다")
    void getTemplate_codex() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.CODEX);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v4");
        assertThat(response.templateMarkdown()).contains("CODEX");
        assertThat(response.templateMarkdown()).contains("start_auto_flow");
        assertThat(response.templateMarkdown()).contains("자동화 워크 플로우 진행해줘");
        assertThat(response.templateMarkdown()).contains("actions.writeFiles");
        assertThat(response.templateMarkdown()).contains("부분 보정");
        assertThat(response.templateMarkdown()).contains("새 문서를 처음부터 다시 작성하지 말고");
        assertThat(response.templateMarkdown()).contains("추천 API를 다시 호출하지 않고, 이미 생성된 skills 파일 기준으로 최종화");
    }

    @Test
    @DisplayName("GEMINI 템플릿을 조회할 수 있다")
    void getTemplate_gemini() {
        StartAgentTemplateResponse response = startAgentTemplateService.getTemplate(AgentType.GEMINI);

        assertThat(response.templateName()).isEqualTo("start.agent.md");
        assertThat(response.version()).isEqualTo("v4");
        assertThat(response.templateMarkdown()).contains("GEMINI");
        assertThat(response.templateMarkdown()).contains("start_auto_flow");
        assertThat(response.templateMarkdown()).contains("자동화 워크 플로우 진행해줘");
        assertThat(response.templateMarkdown()).contains("actions.writeFiles");
        assertThat(response.templateMarkdown()).contains("부분 보정");
        assertThat(response.templateMarkdown()).contains("새 문서를 처음부터 다시 작성하지 말고");
        assertThat(response.templateMarkdown()).contains("추천 API를 다시 호출하지 않고, 이미 생성된 skills 파일 기준으로 최종화");
    }
}
