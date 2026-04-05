package back.domain.mcp.candidate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record McpRecommendationCandidateMetadata(
        Integer stars,
        Integer forks,
        @JsonProperty("updated_at")
        String updatedAt) {}
