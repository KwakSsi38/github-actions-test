package back.domain.prompt.prompt.entity;

import back.domain.prompt.prompt.enums.Category;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "skills")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 연관 객체는 영속성 컨텍스트가 관리한다.")
public class Skill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @Column(name = "content_hash", length = 100)
    private String contentHash;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private Category category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags_json", columnDefinition = "jsonb")
    private Set<String> tagsJson = new HashSet<>();

    @Column(name = "is_chunked")
    private boolean isChunked = false;

    public void update(String contentMd, String contentHash, Set<String> tagsJson, Category category) {
        this.contentMd = contentMd;
        this.contentHash = contentHash;
        this.tagsJson = tagsJson;
        this.category = category;
        this.isChunked = false;
    }

    public void updateTagAndCategory(Set<String> tagsJson, Category category) {
        this.tagsJson = tagsJson;
        this.category = category;
    }
}
