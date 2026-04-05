package back.domain.auth.entity;

import java.time.LocalDateTime;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mcp_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class McpToken extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "token_prefix", nullable = false, length = 20)
    private String tokenPrefix;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    private McpToken(Long memberId, String tokenHash, String tokenPrefix, String name, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.tokenPrefix = tokenPrefix;
        this.name = name;
        this.expiresAt = expiresAt;
    }

    public static McpToken issue(
            Long memberId,
            String tokenHash,
            String tokenPrefix,
            String name,
            LocalDateTime expiresAt) {
        return new McpToken(
                requireNotNull(memberId, "memberId"),
                requireNotBlank(tokenHash, "tokenHash"),
                requireNotBlank(tokenPrefix, "tokenPrefix"),
                requireNotBlank(name, "name"),
                requireNotNull(expiresAt, "expiresAt"));
    }

    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt != null) {
            return;
        }
        this.revokedAt = requireNotNull(revokedAt, "revokedAt");
    }

    public void touch(LocalDateTime lastUsedAt) {
        this.lastUsedAt = requireNotNull(lastUsedAt, "lastUsedAt");
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    private static Long requireNotNull(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static LocalDateTime requireNotNull(LocalDateTime value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
