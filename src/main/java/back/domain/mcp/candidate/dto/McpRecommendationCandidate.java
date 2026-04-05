package back.domain.mcp.candidate.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record McpRecommendationCandidate(
        @JsonProperty("skill_id")
        Long skillId,

        @JsonProperty("skill_name")
        String skillName,

        @JsonProperty("repository_name")
        String repositoryName,

        @JsonProperty("repository_url")
        String repositoryUrl,

        @JsonProperty("content_md")
        @JsonAlias("skill_md_raw")
        String contentMd,

        String category,

        String summary,

        @JsonProperty("primary_score")
        Double primaryScore,

        McpRecommendationCandidateMetadata metadata) {}
