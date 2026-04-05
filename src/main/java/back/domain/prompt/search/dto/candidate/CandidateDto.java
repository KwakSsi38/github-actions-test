package back.domain.prompt.search.dto.candidate;

import back.domain.prompt.prompt.enums.Category;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CandidateDto(

        @JsonProperty("skill_id")
        Long skillId,

        @JsonProperty("skill_name")
        String skillName,

        @JsonProperty("repository_name")
        String repositoryName,

        @JsonProperty("repository_url")
        String repositoryUrl,

        @JsonProperty("content_md")
        String contentMd,

        Category category,

        String summary,

        @JsonProperty("primary_score")
        float primaryScore,

        CandidateMetadataDto metadata

) {}