package back.domain.auth.dto.response;

import java.time.LocalDateTime;

public record CreateMcpTokenResponse(
        long tokenId,
        String token,
        String tokenPrefix,
        String name,
        LocalDateTime expiresAt) {}
