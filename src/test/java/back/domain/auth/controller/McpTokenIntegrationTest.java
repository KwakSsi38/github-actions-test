package back.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.security.JwtTokenProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class McpTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private McpTokenRepository mcpTokenRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("MCP 토큰 발급 API는 인증이 없으면 401을 반환한다")
    void issueToken_withoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/mcp/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Claude Desktop"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("MCP 토큰 발급 성공 시 평문 토큰을 반환하고 DB에는 해시가 저장된다")
    void issueToken_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-701", "u701@example.com", "User 701"));
        String accessToken = issueAccessToken(member);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/mcp/tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Claude Desktop"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("MCP 토큰 발급 성공"))
                .andExpect(jsonPath("$.data.tokenId").isNumber())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.tokenPrefix").isString())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        String rawToken = jsonNode.get("data").get("token").asText();
        long tokenId = jsonNode.get("data").get("tokenId").asLong();

        McpToken savedToken = mcpTokenRepository.findById(tokenId).orElseThrow();
        assertThat(savedToken.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(savedToken.getTokenPrefix()).isEqualTo(rawToken.substring(0, Math.min(rawToken.length(), 12)));
    }

    @Test
    @DisplayName("MCP 토큰 목록 조회는 본인 토큰 목록을 반환한다")
    void getTokens_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-702", "u702@example.com", "User 702"));
        String accessToken = issueAccessToken(member);
        McpToken token = McpToken.issue(
                member.getId(),
                "hash_value",
                "mcp_hash_pre",
                "Codex",
                LocalDateTime.now().plusDays(5));
        mcpTokenRepository.save(token);

        mockMvc.perform(get("/api/v1/mcp/tokens")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.tokens").isArray())
                .andExpect(jsonPath("$.data.tokens[0].name").value("Codex"))
                .andExpect(jsonPath("$.data.tokens[0].revoked").value(false));
    }

    @Test
    @DisplayName("본인 소유가 아닌 MCP 토큰 폐기 요청은 403을 반환한다")
    void revokeToken_whenOwnerMismatch() throws Exception {
        Member owner = memberRepository.save(Member.createUser("google-sub-703", "u703@example.com", "User 703"));
        Member attacker = memberRepository.save(Member.createUser("google-sub-704", "u704@example.com", "User 704"));
        String attackerAccessToken = issueAccessToken(attacker);
        McpToken ownerToken = mcpTokenRepository.save(McpToken.issue(
                owner.getId(),
                "owner_hash_value",
                "mcp_owner",
                "Owner Token",
                LocalDateTime.now().plusDays(3)));

        mockMvc.perform(delete("/api/v1/mcp/tokens/{tokenId}", ownerToken.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(attackerAccessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("본인 토큰이 아닙니다."));
    }

    @Test
    @DisplayName("본인 토큰 폐기 요청은 성공하고 revoked 상태가 된다")
    void revokeToken_success() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-705", "u705@example.com", "User 705"));
        String accessToken = issueAccessToken(member);
        McpToken token = mcpTokenRepository.save(McpToken.issue(
                member.getId(),
                "hash_value_705",
                "mcp_705",
                "My Token",
                LocalDateTime.now().plusDays(5)));

        mockMvc.perform(delete("/api/v1/mcp/tokens/{tokenId}", token.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("MCP 토큰 폐기 성공"));

        McpToken revokedToken = mcpTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(revokedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 MCP 토큰 폐기 요청은 404를 반환한다")
    void revokeToken_whenTokenNotFound() throws Exception {
        Member member = memberRepository.save(Member.createUser("google-sub-706", "u706@example.com", "User 706"));
        String accessToken = issueAccessToken(member);

        mockMvc.perform(delete("/api/v1/mcp/tokens/{tokenId}", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("토큰이 존재하지 않습니다."));
    }

    private String issueAccessToken(Member member) {
        return jwtTokenProvider.generateAccessToken(member.getId(), member.getEmail(), member.getRole().name());
    }
}
