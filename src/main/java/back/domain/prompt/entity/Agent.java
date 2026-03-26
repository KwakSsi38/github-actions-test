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
@Table(name = "agents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Agent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false, unique = true)
    @SuppressFBWarnings("EI_EXPOSE_REP")
    private Repository repository;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "content_hash", length = 100)
    private String contentHash;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    public static Agent create(Repository repository, String contentMd, String contentHash, String filePath) {
        Agent agent = new Agent();
        agent.repository = repository;
        agent.contentMd = contentMd;
        agent.contentHash = contentHash;
        agent.filePath = filePath;
        return agent;
    }

    public void update(String contentMd, String contentHash) {
        this.contentMd = contentMd;
        this.contentHash = contentHash;
        setUpdatedAt(LocalDateTime.now());
    }
}