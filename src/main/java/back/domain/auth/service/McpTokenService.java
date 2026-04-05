package back.domain.auth.service;

import java.time.LocalDateTime;

import back.domain.auth.dto.response.CreateMcpTokenResponse;
import back.domain.auth.dto.response.McpTokenListResponse;

public interface McpTokenService {
    CreateMcpTokenResponse issueToken(long memberId, String name, LocalDateTime expiresAt);

    McpTokenListResponse getTokens(long memberId);

    void revokeToken(long memberId, long tokenId);
}
