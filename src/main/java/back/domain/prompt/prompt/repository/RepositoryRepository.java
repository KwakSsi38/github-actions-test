package back.domain.prompt.prompt.repository;

import back.domain.prompt.prompt.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    // github_id로 조회
    Optional<Repository> findByGithubId(Long githubId);

}
