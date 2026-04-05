package back.domain.prompt.prompt.repository;

import back.domain.prompt.prompt.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    // content_hash 기준 변경 감지
    Optional<Skill> findByRepositoryIdAndName(Long repositoryId, String name);

    List<Skill> findAllByOrderByIdAsc();

    List<Skill> findByIsChunkedFalse();

    @Modifying
    @Transactional
    @Query("UPDATE Skill s SET s.isChunked = true WHERE s.id = :id")
    void markAsChunked(@Param("id") Long id);
}
