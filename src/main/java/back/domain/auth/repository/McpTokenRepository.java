package back.domain.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import back.domain.auth.entity.McpToken;

public interface McpTokenRepository extends JpaRepository<McpToken, Long> {
    List<McpToken> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<McpToken> findByTokenHash(String tokenHash);
}
