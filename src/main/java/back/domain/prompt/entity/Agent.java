package back.domain.prompt.entity;

import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Agent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false, unique = true)
    private Repository repository;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "content_hash", length = 100)
    private String contentHash;  // 파일 SHA 해시값

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    // 내용 변경 시 업데이트
    public void update(String contentMd, String contentHash) {
        this.contentMd = contentMd;
        this.contentHash = contentHash;
        setUpdatedAt(LocalDateTime.now());
    }
}
