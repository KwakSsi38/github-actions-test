package back.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.auth.dto.response.CreateMcpTokenResponse;
import back.domain.auth.dto.response.McpTokenListResponse;
import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.auth.util.McpTokenGenerator;
import back.domain.auth.util.McpTokenHasher;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class McpTokenServiceTest {

    @Mock
    private McpTokenRepository mcpTokenRepository;

    @Mock
    private McpTokenGenerator mcpTokenGenerator;

    @Mock
    private McpTokenHasher mcpTokenHasher;

    private McpTokenService mcpTokenService;

    @BeforeEach
    void setUp() {
        McpTokenServiceImpl service = new McpTokenServiceImpl(mcpTokenRepository, mcpTokenGenerator, mcpTokenHasher);
        ReflectionTestUtils.setField(service, "defaultTokenExpirationDays", 90L);
        mcpTokenService = service;
    }

    @Test
    @DisplayName("MCP 토큰 발급 시 평문 토큰을 응답하고 해시를 저장한다")
    void issueToken_success() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        when(mcpTokenGenerator.generate()).thenReturn("mcp_generated_token");
        when(mcpTokenHasher.hash("mcp_generated_token")).thenReturn("hashed_token");
        when(mcpTokenRepository.save(any(McpToken.class))).thenAnswer(invocation -> {
            McpToken token = invocation.getArgument(0);
            ReflectionTestUtils.setField(token, "id", 1L);
            return token;
        });

        CreateMcpTokenResponse response = mcpTokenService.issueToken(10L, "Claude Desktop", expiresAt);

        assertThat(response.tokenId()).isEqualTo(1L);
        assertThat(response.token()).isEqualTo("mcp_generated_token");
        assertThat(response.tokenPrefix()).isEqualTo("mcp_generate");
        assertThat(response.name()).isEqualTo("Claude Desktop");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("만료 시각이 없으면 기본 만료일(90일)을 적용한다")
    void issueToken_withoutExpiresAt_usesDefaultDays() {
        LocalDateTime beforeIssue = LocalDateTime.now();
        when(mcpTokenGenerator.generate()).thenReturn("mcp_generated_token");
        when(mcpTokenHasher.hash("mcp_generated_token")).thenReturn("hashed_token");
        when(mcpTokenRepository.save(any(McpToken.class))).thenAnswer(invocation -> {
            McpToken token = invocation.getArgument(0);
            ReflectionTestUtils.setField(token, "id", 2L);
            return token;
        });

        CreateMcpTokenResponse response = mcpTokenService.issueToken(10L, "Codex", null);

        assertThat(response.expiresAt()).isAfter(beforeIssue.plusDays(89));
        assertThat(response.expiresAt()).isBefore(beforeIssue.plusDays(91));
    }

    @Test
    @DisplayName("만료 시각이 현재보다 과거면 400 예외를 던진다")
    void issueToken_whenExpiresAtIsPast_throwsBadRequest() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);

        assertThatThrownBy(() -> mcpTokenService.issueToken(10L, "Codex", past))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.BAD_REQUEST);
                });
    }

    @Test
    @DisplayName("토큰 목록 조회 시 본인 토큰만 요약해서 반환한다")
    void getTokens_success() {
        McpToken first = McpToken.issue(
                10L, "hash1", "mcp_abc", "Claude Desktop", LocalDateTime.now().plusDays(10));
        ReflectionTestUtils.setField(first, "id", 1L);
        McpToken second = McpToken.issue(
                10L, "hash2", "mcp_def", "Codex", LocalDateTime.now().plusDays(20));
        ReflectionTestUtils.setField(second, "id", 2L);
        second.revoke(LocalDateTime.now());
        when(mcpTokenRepository.findAllByMemberIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(second, first));

        McpTokenListResponse response = mcpTokenService.getTokens(10L);

        assertThat(response.tokens()).hasSize(2);
        assertThat(response.tokens().get(0).tokenId()).isEqualTo(2L);
        assertThat(response.tokens().get(0).revoked()).isTrue();
        assertThat(response.tokens().get(1).tokenId()).isEqualTo(1L);
        assertThat(response.tokens().get(1).revoked()).isFalse();
    }

    @Test
    @DisplayName("본인 소유가 아닌 토큰 폐기 요청은 403 예외를 던진다")
    void revokeToken_whenOwnerMismatch_throwsForbidden() {
        McpToken token = McpToken.issue(
                20L, "hash1", "mcp_abc", "Owner Token", LocalDateTime.now().plusDays(10));
        ReflectionTestUtils.setField(token, "id", 1L);
        when(mcpTokenRepository.findById(1L)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> mcpTokenService.revokeToken(10L, 1L))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("본인 토큰 폐기 요청은 revokedAt을 기록한다")
    void revokeToken_success() {
        McpToken token = McpToken.issue(
                10L, "hash1", "mcp_abc", "My Token", LocalDateTime.now().plusDays(10));
        ReflectionTestUtils.setField(token, "id", 1L);
        when(mcpTokenRepository.findById(1L)).thenReturn(Optional.of(token));

        mcpTokenService.revokeToken(10L, 1L);

        verify(mcpTokenRepository).save(token);
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 토큰 폐기 요청은 404 예외를 던진다")
    void revokeToken_whenTokenNotFound_throwsNotFound() {
        when(mcpTokenRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpTokenService.revokeToken(10L, 999L))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
                });
    }
}
