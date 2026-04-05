package back.domain.prompt.prompt.entity;

import back.domain.prompt.prompt.enums.OwnerType;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "repositories")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "JPA 엔티티의 연관 객체와 JSON 컬렉션/맵 필드는 영속성 컨텍스트가 관리한다.")
public class Repository extends BaseEntity {

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "source_repo", nullable = false, length = 255)
    private String sourceRepo;  // owner/repo

    @Column(name = "source_uri", nullable = false, unique = true, length = 500)
    private String sourceUri;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "tags_json", columnDefinition = "jsonb")
//    private Set<String> tagsJson = new HashSet<>();

    @Column(name = "star_count")
    private Integer starCount;

    @Column(name = "fork_count")
    private Integer forkCount;

    @Column(name = "size")
    private Integer size;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "language_stats", columnDefinition = "jsonb")
    private Map<String, Integer> languageStats = new HashMap<>();

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
    private Map<String, Object> rawMetadata = new HashMap<>();

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills = new ArrayList<>();

    @OneToOne(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    private Agent agent;

    public void update(
            Integer starCount,
            Integer forkCount,
            String etag,
            LocalDateTime sourceUpdatedAt,
            String summary,
            String homepage,
            String license,
            String ownerAvatarUrl,
            boolean active,
            Map<String, Object> rawMetadata,
            Map<String, Integer> languageStats
    ) {
        this.starCount = starCount;
        this.forkCount = forkCount;
        this.etag = etag;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.summary = summary;
        this.homepage = homepage;
        this.license = license;
        this.ownerAvatarUrl = ownerAvatarUrl;
        this.active = active;
        this.rawMetadata = rawMetadata;
        this.languageStats = languageStats;
    }

    public void deactivate() {
        this.active = false;
    }

}
