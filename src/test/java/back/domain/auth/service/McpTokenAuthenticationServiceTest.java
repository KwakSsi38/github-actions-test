package back.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.auth.util.McpTokenHasher;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class McpTokenAuthenticationServiceTest {

    @Mock
    private McpTokenRepository mcpTokenRepository;

    @Mock
    private McpTokenHasher mcpTokenHasher;

    private McpTokenAuthenticationService mcpTokenAuthenticationService;

    @BeforeEach
    void setUp() {
        mcpTokenAuthenticationService = new McpTokenAuthenticationServiceImpl(mcpTokenRepository, mcpTokenHasher);
    }

    @Test
    @DisplayName("유효한 MCP 토큰이면 memberId를 반환하고 lastUsedAt을 갱신한다")
    void authenticate_success() {
        String rawToken = "mcp_valid_token";
        String tokenHash = "hashed_token";
        McpToken mcpToken = McpToken.issue(
                10L,
                tokenHash,
                "mcp_valid_to",
                "Claude Desktop",
                LocalDateTime.now().plusDays(1));

        when(mcpTokenHasher.hash(rawToken)).thenReturn(tokenHash);
        when(mcpTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(mcpToken));

        long memberId = mcpTokenAuthenticationService.authenticate("Bearer " + rawToken);

        assertThat(memberId).isEqualTo(10L);
        assertThat(mcpToken.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 없으면 401 예외를 던진다")
    void authenticate_whenTokenNotFound_throwsUnauthorized() {
        String rawToken = "mcp_unknown_token";
        when(mcpTokenHasher.hash(rawToken)).thenReturn("unknown_hash");
        when(mcpTokenRepository.findByTokenHash("unknown_hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mcpTokenAuthenticationService.authenticate("Bearer " + rawToken))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
                    assertThat(serviceException.getClientMessage()).isEqualTo("MCP 토큰 인증 실패");
                });
    }

    @Test
    @DisplayName("만료 또는 폐기된 토큰이면 401 예외를 던진다")
    void authenticate_whenTokenExpiredOrRevoked_throwsUnauthorized() {
        String rawToken = "mcp_expired_token";
        String tokenHash = "expired_hash";
        McpToken expiredToken = McpToken.issue(
                10L,
                tokenHash,
                "mcp_expired_",
                "Expired",
                LocalDateTime.now().minusMinutes(1));

        when(mcpTokenHasher.hash(rawToken)).thenReturn(tokenHash);
        when(mcpTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> mcpTokenAuthenticationService.authenticate("Bearer " + rawToken))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
                    assertThat(serviceException.getClientMessage()).isEqualTo("MCP 토큰 인증 실패");
                });
    }
}
