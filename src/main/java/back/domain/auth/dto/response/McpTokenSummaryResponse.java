package back.domain.auth.dto.response;

import java.time.LocalDateTime;

public record McpTokenSummaryResponse(
        long tokenId,
        String name,
        String tokenPrefix,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        boolean revoked) {}
