package back.domain.prompt.entity;

import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "skills")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Skill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    @SuppressFBWarnings("EI_EXPOSE_REP")
    private Repository repository;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "content_hash", length = 100)
    private String contentHash;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    public static Skill create(
            Repository repository,
            String name,
            String contentMd,
            String contentHash,
            String filePath
    ) {
        Skill skill = new Skill();
        skill.repository = repository;
        skill.name = name;
        skill.contentMd = contentMd;
        skill.contentHash = contentHash;
        skill.filePath = filePath;
        return skill;
    }

    public void update(String contentMd, String contentHash) {
        this.contentMd = contentMd;
        this.contentHash = contentHash;
        setUpdatedAt(LocalDateTime.now());
    }
}
