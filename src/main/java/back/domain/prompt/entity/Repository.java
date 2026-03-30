package back.domain.prompt.entity;

import back.domain.prompt.enums.OwnerType;
import back.global.jpa.entity.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "repositories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags_json", columnDefinition = "jsonb")
    private Set<String> tagsJson = new HashSet<>();

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

    public static Repository create(
            Long githubId,
            String name,
            String sourceRepo,
            String sourceUri,
            String summary,
            Set<String> tagsJson,
            Integer starCount,
            Integer forkCount,
            Integer size,
            Map<String, Integer> languageStats,
            String license,
            String homepage,
            String ownerAvatarUrl,
            OwnerType ownerType,
            Boolean isOfficial,
            String defaultBranch,
            String etag,
            LocalDateTime sourceUpdatedAt,
            Boolean active,
            Map<String, Object> rawMetadata
    ) {
        Repository repository = new Repository();
        repository.githubId = githubId;
        repository.name = name;
        repository.sourceRepo = sourceRepo;
        repository.sourceUri = sourceUri;
        repository.summary = summary;
        repository.tagsJson = tagsJson == null ? new HashSet<>() : new HashSet<>(tagsJson);
        repository.starCount = starCount;
        repository.forkCount = forkCount;
        repository.size = size;
        repository.languageStats = languageStats == null ? new HashMap<>() : new HashMap<>(languageStats);
        repository.license = license;
        repository.homepage = homepage;
        repository.ownerAvatarUrl = ownerAvatarUrl;
        repository.ownerType = ownerType;
        repository.isOfficial = isOfficial;
        repository.defaultBranch = defaultBranch;
        repository.etag = etag;
        repository.sourceUpdatedAt = sourceUpdatedAt;
        repository.active = active != null ? active : Boolean.TRUE;
        repository.rawMetadata = rawMetadata == null ? new HashMap<>() : new HashMap<>(rawMetadata);
        return repository;
    }

    public void update(
            Integer starCount,
            Integer forkCount,
            String etag,
            LocalDateTime sourceUpdatedAt
    ) {
        this.starCount = starCount;
        this.forkCount = forkCount;
        this.etag = etag;
        this.sourceUpdatedAt = sourceUpdatedAt;
        setUpdatedAt(LocalDateTime.now());
    }

    public void deactivate() {
        this.active = false;
    }

    public Set<String> getTagsJson() {
        return new HashSet<>(tagsJson);
    }

    public Map<String, Integer> getLanguageStats() {
        return new HashMap<>(languageStats);
    }

    public Map<String, Object> getRawMetadata() {
        return new HashMap<>(rawMetadata);
    }

    public List<Skill> getSkills() {
        return new ArrayList<>(skills);
    }
}
