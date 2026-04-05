package back.domain.mcp.recommendation.controller;

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
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = "app.mcp.recommendation.candidate-source=mock")
class McpRecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private McpTokenRepository mcpTokenRepository;

    @Autowired
    private McpTokenHasher mcpTokenHasher;

    @Test
    @DisplayName("유효한 MCP 토큰으로 추천 API를 호출하면 추천 결과를 반환한다")
    void recommend_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-901", "u901@example.com", "User 901"));
        String rawMcpToken = "mcp_recommend_token_901";

        McpToken mcpToken = McpToken.issue(
                member.getId(),
                mcpTokenHasher.hash(rawMcpToken),
                rawMcpToken.substring(0, Math.min(rawMcpToken.length(), 12)),
                "Codex",
                LocalDateTime.now().plusDays(3));
        mcpTokenRepository.save(mcpToken);

        mockMvc.perform(post("/api/v1/mcp/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(rawMcpToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keywords": "SpringBoot infra DevOps"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("추천 성공"))
                .andExpect(jsonPath("$.data.selectedSkills").isArray());
    }

    @Test
    @DisplayName("유효하지 않은 MCP 토큰이면 401을 반환한다")
    void recommend_whenInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/mcp/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mcp_invalid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keywords": "SpringBoot"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("MCP 토큰 인증 실패"));
    }

    @Test
    @DisplayName("keywords가 공백이면 400을 반환한다")
    void recommend_whenKeywordsIsBlank() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-902", "u902@example.com", "User 902"));
        String rawMcpToken = "mcp_recommend_token_902";

        McpToken mcpToken = McpToken.issue(
                member.getId(),
                mcpTokenHasher.hash(rawMcpToken),
                rawMcpToken.substring(0, Math.min(rawMcpToken.length(), 12)),
                "Codex",
                LocalDateTime.now().plusDays(3));
        mcpTokenRepository.save(mcpToken);

        mockMvc.perform(post("/api/v1/mcp/recommendations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(rawMcpToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "keywords": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
