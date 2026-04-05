package back.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.domain.auth.dto.request.CreateMcpTokenRequest;
import back.domain.auth.dto.response.CreateMcpTokenResponse;
import back.domain.auth.dto.response.McpTokenListResponse;
import back.domain.auth.service.McpTokenService;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.response.RsData;
import back.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/mcp/tokens")
@Validated
@RequiredArgsConstructor
public class McpTokenController {
    private final McpTokenService mcpTokenService;

    @PostMapping
    public ResponseEntity<RsData<CreateMcpTokenResponse>> issueToken(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateMcpTokenRequest request) {
        long memberId = resolveAuthenticatedMemberId(authenticatedMember);
        CreateMcpTokenResponse response = mcpTokenService.issueToken(memberId, request.name(), request.expiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RsData<>(response, "MCP 토큰 발급 성공"));
    }

    @GetMapping
    public ResponseEntity<RsData<McpTokenListResponse>> getTokens(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {
        long memberId = resolveAuthenticatedMemberId(authenticatedMember);
        McpTokenListResponse response = mcpTokenService.getTokens(memberId);
        return ResponseEntity.ok(new RsData<>(response, "조회 성공"));
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<RsData<Void>> revokeToken(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable @Positive(message = "tokenId-Positive-tokenId는 1 이상이어야 합니다.") Long tokenId) {
        long memberId = resolveAuthenticatedMemberId(authenticatedMember);
        mcpTokenService.revokeToken(memberId, tokenId);
        return ResponseEntity.ok(new RsData<>("MCP 토큰 폐기 성공"));
    }

    private long resolveAuthenticatedMemberId(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new ServiceException(
                    CommonErrorCode.UNAUTHORIZED,
                    "[McpTokenController#resolveAuthenticatedMemberId] authenticated member is missing",
                    CommonErrorCode.UNAUTHORIZED.defaultMessage());
        }

        return authenticatedMember.memberId();
    }
}
