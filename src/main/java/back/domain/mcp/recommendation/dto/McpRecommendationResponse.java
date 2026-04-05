package back.domain.mcp.recommendation.dto;

import java.util.List;

public record McpRecommendationResponse(List<McpRecommendedSkillResponse> selectedSkills) {
    public McpRecommendationResponse {
        selectedSkills = selectedSkills == null ? List.of() : List.copyOf(selectedSkills);
    }
}
