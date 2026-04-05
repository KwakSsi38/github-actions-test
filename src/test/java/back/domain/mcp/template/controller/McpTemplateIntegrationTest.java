package back.domain.mcp.template.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.auth.util.McpTokenHasher;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class McpTemplateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private McpTokenRepository mcpTokenRepository;

    @Autowired
    private McpTokenHasher mcpTokenHasher;

    @Test
    @DisplayName("유효한 MCP 토큰으로 start-agent 템플릿을 조회할 수 있다")
    void getStartAgentTemplate_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-801", "u801@example.com", "User 801"));
        String rawMcpToken = "mcp_template_token_801";

        McpToken mcpToken = McpToken.issue(
                member.getId(),
                mcpTokenHasher.hash(rawMcpToken),
                rawMcpToken.substring(0, Math.min(rawMcpToken.length(), 12)),
                "Claude Desktop",
                LocalDateTime.now().plusDays(3));
        mcpTokenRepository.save(mcpToken);

        mockMvc.perform(post("/api/v1/mcp/template/start-agent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(rawMcpToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentType": "CLAUDE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("템플릿 조회 성공"))
                .andExpect(jsonPath("$.data.templateName").value("start.agent.md"))
                .andExpect(jsonPath("$.data.version").value("v4"))
                .andExpect(jsonPath("$.data.templateMarkdown").isString());
    }

    @Test
    @DisplayName("유효하지 않은 MCP 토큰으로 요청하면 401을 반환한다")
    void getStartAgentTemplate_whenInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/mcp/template/start-agent")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mcp_invalid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentType": "CLAUDE"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("MCP 토큰 인증 실패"));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환한다")
    void getStartAgentTemplate_withoutAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/mcp/template/start-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentType": "CLAUDE"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("MCP 토큰 인증 실패"));
    }
}
