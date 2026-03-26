package back.domain.prompt.repository;

import back.domain.prompt.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    // github_id 기준 중복 체크
    boolean existsByGithubId(Long githubId);

    // github_id로 조회
    Optional<Repository> findByGithubId(Long githubId);

    // source_repo로 조회 ("octocat/my-repo")
    Optional<Repository> findBySourceRepo(String sourceRepo);
}
