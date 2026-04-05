package back.domain.prompt.prompt.entity;

import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "agents")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 연관 객체는 영속성 컨텍스트가 관리한다.")
public class Agent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false, unique = true)
    private Repository repository;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "content_hash", length = 100)
    private String contentHash;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    public void update(String contentMd, String contentHash) {
        this.contentMd = contentMd;
        this.contentHash = contentHash;
    }
}
