package back.domain.prompt.repository;

import back.domain.prompt.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {

    // 레포당 하나만 존재
    Optional<Agent> findByRepositoryId(Long repositoryId);

    // 존재 여부 확인
    boolean existsByRepositoryId(Long repositoryId);
}
