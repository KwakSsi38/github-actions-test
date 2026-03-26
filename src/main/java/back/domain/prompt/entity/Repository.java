package back.domain.prompt.entity;

import back.domain.prompt.enums.OwnerType;
import back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "repositories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Repository extends BaseEntity {

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "source_repo", nullable = false, length = 255)
    private String sourceRepo;  // "owner/repo" 형식

    @Column(name = "source_uri", nullable = false, unique = true, length = 500)
    private String sourceUri;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags_json", columnDefinition = "jsonb")
    private Set<String> tagsJson;

    @Column(name = "star_count")
    private Integer starCount;

    @Column(name = "fork_count")
    private Integer forkCount;

    @Column(name = "size")
    private Integer size;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "language_stats", columnDefinition = "jsonb")
    private Map<String, Integer> languageStats;

    @Column(name = "license", length = 50)
    private String license;

    @Column(name = "homepage", length = 500)
    private String homepage;

    @Column(name = "owner_avatar_url", length = 500)
    private String ownerAvatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private OwnerType ownerType;

    @Column(name = "is_official")
    private Boolean isOfficial;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(name = "etag", length = 255)
    private String etag;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_metadata", columnDefinition = "jsonb")
    private Map<String, Object> rawMetadata;

    // 연관관계
    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Skill> skills = new ArrayList<>();

    @OneToOne(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    private Agent agent;

    // 변경 감지 후 업데이트
    public void update(Integer starCount, Integer forkCount, String etag,
                       LocalDateTime sourceUpdatedAt) {
        this.starCount = starCount;
        this.forkCount = forkCount;
        this.etag = etag;
        this.sourceUpdatedAt = sourceUpdatedAt;
        setUpdatedAt(LocalDateTime.now());
    }

    public void deactivate() {
        this.active = false;
    }
}
