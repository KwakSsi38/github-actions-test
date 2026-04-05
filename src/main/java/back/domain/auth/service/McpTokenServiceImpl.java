package back.domain.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import back.domain.auth.dto.response.CreateMcpTokenResponse;
import back.domain.auth.dto.response.McpTokenListResponse;
import back.domain.auth.dto.response.McpTokenSummaryResponse;
import back.domain.auth.entity.McpToken;
import back.domain.auth.repository.McpTokenRepository;
import back.domain.auth.util.McpTokenGenerator;
import back.domain.auth.util.McpTokenHasher;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;

@Service
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "스프링 DI로 주입되는 빈 참조이며, 의도된 패턴입니다.")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class McpTokenServiceImpl implements McpTokenService {
    private static final String TOKEN_NOT_FOUND_MESSAGE = "토큰이 존재하지 않습니다.";
    private static final String TOKEN_OWNER_MISMATCH_MESSAGE = "본인 토큰이 아닙니다.";
    private static final String INVALID_EXPIRES_AT_MESSAGE = "만료 시각은 현재 시각 이후여야 합니다.";
    private static final int TOKEN_PREFIX_PREVIEW_LENGTH = 12;

    private final McpTokenRepository mcpTokenRepository;
    private final McpTokenGenerator mcpTokenGenerator;
    private final McpTokenHasher mcpTokenHasher;

    @Value("${custom.mcp.token-expiration-days:90}")
    private long defaultTokenExpirationDays;

    @Override
    @Transactional
    public CreateMcpTokenResponse issueToken(long memberId, String name, LocalDateTime expiresAt) {
        validateMemberId(memberId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolvedExpiresAt = resolveExpiresAt(expiresAt, now);

        String rawToken = mcpTokenGenerator.generate();
        String tokenHash = mcpTokenHasher.hash(rawToken);
        String tokenPrefix = extractTokenPrefix(rawToken);

        McpToken mcpToken = McpToken.issue(memberId, tokenHash, tokenPrefix, name, resolvedExpiresAt);
        McpToken savedToken = mcpTokenRepository.save(mcpToken);

        return new CreateMcpTokenResponse(
                savedToken.getId(),
                rawToken,
                savedToken.getTokenPrefix(),
                savedToken.getName(),
                savedToken.getExpiresAt());
    }

    @Override
    public McpTokenListResponse getTokens(long memberId) {
        validateMemberId(memberId);
        List<McpTokenSummaryResponse> summaries = mcpTokenRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();

        return new McpTokenListResponse(summaries);
    }

    @Override
    @Transactional
    public void revokeToken(long memberId, long tokenId) {
        validateMemberId(memberId);
        McpToken mcpToken = mcpTokenRepository.findById(tokenId).orElseThrow(() -> new ServiceException(
                CommonErrorCode.NOT_FOUND,
                "[McpTokenServiceImpl#revokeToken] token not found",
                TOKEN_NOT_FOUND_MESSAGE));

        if (!mcpToken.getMemberId().equals(memberId)) {
            throw new ServiceException(
                    CommonErrorCode.FORBIDDEN,
                    "[McpTokenServiceImpl#revokeToken] token owner and authenticated member do not match",
                    TOKEN_OWNER_MISMATCH_MESSAGE);
        }

        mcpToken.revoke(LocalDateTime.now());
        mcpTokenRepository.save(mcpToken);
    }

    private McpTokenSummaryResponse toSummaryResponse(McpToken mcpToken) {
        return new McpTokenSummaryResponse(
                mcpToken.getId(),
                mcpToken.getName(),
                mcpToken.getTokenPrefix(),
                mcpToken.getExpiresAt(),
                mcpToken.getLastUsedAt(),
                mcpToken.isRevoked());
    }

    private LocalDateTime resolveExpiresAt(LocalDateTime requestedExpiresAt, LocalDateTime now) {
        LocalDateTime resolvedExpiresAt =
                requestedExpiresAt == null ? now.plusDays(defaultTokenExpirationDays) : requestedExpiresAt;

        if (!resolvedExpiresAt.isAfter(now)) {
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST,
                    "[McpTokenServiceImpl#resolveExpiresAt] expiresAt must be after current time",
                    INVALID_EXPIRES_AT_MESSAGE);
        }

        return resolvedExpiresAt;
    }

    private void validateMemberId(long memberId) {
        if (memberId <= 0) {
            throw new ServiceException(
                    CommonErrorCode.BAD_REQUEST,
                    "[McpTokenServiceImpl#validateMemberId] memberId must be positive",
                    CommonErrorCode.BAD_REQUEST.defaultMessage());
        }
    }

    private String extractTokenPrefix(String token) {
        int endIndex = Math.min(token.length(), TOKEN_PREFIX_PREVIEW_LENGTH);
        return token.substring(0, endIndex);
    }
}
