package back.domain.mcp.recommendation.ranker;

import java.util.List;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;

public interface McpRecommendationRanker {
    List<McpRecommendedSkillResponse> rank(List<McpRecommendationCandidate> candidates);
}
