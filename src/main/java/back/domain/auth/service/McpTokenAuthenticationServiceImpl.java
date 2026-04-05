package back.domain.auth.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.auth.util.McpTokenHasher;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class McpTokenAuthenticationServiceImpl implements McpTokenAuthenticationService {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MCP_PREFIX = "mcp_";
    private static final String MCP_AUTH_FAILED_MESSAGE = "MCP 토큰 인증 실패";

    private final McpTokenRepository mcpTokenRepository;
    private final McpTokenHasher mcpTokenHasher;

    @Override
    @Transactional
    public long authenticate(String authorizationHeader) {
        String rawMcpToken = resolveRawMcpToken(authorizationHeader);
        String tokenHash = mcpTokenHasher.hash(rawMcpToken);

        McpToken mcpToken = mcpTokenRepository.findByTokenHash(tokenHash).orElseThrow(() -> new ServiceException(
                CommonErrorCode.UNAUTHORIZED,
                "[McpTokenAuthenticationServiceImpl#authenticate] mcp token not found",
                MCP_AUTH_FAILED_MESSAGE));

        LocalDateTime now = LocalDateTime.now();
        if (mcpToken.isRevoked() || !mcpToken.getExpiresAt().isAfter(now)) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[McpTokenAuthenticationServiceImpl#authenticate] mcp token is revoked or expired",
                    MCP_AUTH_FAILED_MESSAGE);
        }

        mcpToken.touch(now);
        return mcpToken.getMemberId();
    }

    private String resolveRawMcpToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[McpTokenAuthenticationServiceImpl#resolveRawMcpToken] authorization header is missing",
                    MCP_AUTH_FAILED_MESSAGE);
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[McpTokenAuthenticationServiceImpl#resolveRawMcpToken] authorization header format is invalid",
                    MCP_AUTH_FAILED_MESSAGE);
        }

        String rawToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (rawToken.isBlank() || !rawToken.startsWith(MCP_PREFIX)) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[McpTokenAuthenticationServiceImpl#resolveRawMcpToken] mcp token format is invalid",
                    MCP_AUTH_FAILED_MESSAGE);
        }

        return rawToken;
    }
}
