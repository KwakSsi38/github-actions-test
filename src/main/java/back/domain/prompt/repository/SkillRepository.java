package back.domain.prompt.repository;

import back.domain.prompt.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    // 레포에 속한 스킬 전체 조회
    List<Skill> findAllByRepositoryId(Long repositoryId);

    // content_hash 기준 변경 감지
    Optional<Skill> findByRepositoryIdAndName(Long repositoryId, String name);

}
