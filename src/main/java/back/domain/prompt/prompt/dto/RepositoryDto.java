package back.domain.prompt.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Jackson DTO는 JSON 컬렉션/맵 값을 그대로 전달한다.")
public class RepositoryDto {

    @JsonProperty("github_id")
    private Long githubId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("source_repo")
    private String sourceRepo;

    @JsonProperty("source_url")
    private String sourceUrl;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("star_count")
    private Integer starCount;

    @JsonProperty("fork_count")
    private Integer forkCount;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("language_stats")
    private Map<String, Integer> languageStats;

    @JsonProperty("license")
    private String license;

    @JsonProperty("homepage")
    private String homepage;

    @JsonProperty("owner_avatar_url")
    private String ownerAvatarUrl;

    @JsonProperty("owner_type")
    private String ownerType;

    @JsonProperty("is_official")
    private Boolean isOfficial;

    @JsonProperty("default_branch")
    private String defaultBranch;

    @JsonProperty("etag")
    private String etag;

    @JsonProperty("source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("raw_metadata")
    private Map<String, Object> rawMetadata;
}
