package back.domain.prompt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class RepositoryData {

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

    public Map<String, Object> getRawMetadata() {
        return rawMetadata == null ? null : new HashMap<>(rawMetadata);
    }

    public Map<String, Integer> getLanguageStats() {
        return languageStats == null ? null : new HashMap<>(languageStats);
    }
}