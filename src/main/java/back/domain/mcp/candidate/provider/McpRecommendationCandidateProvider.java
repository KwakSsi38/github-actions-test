package back.domain.mcp.candidate.provider;

import java.util.List;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;

public interface McpRecommendationCandidateProvider {
    List<McpRecommendationCandidate> findTopCandidates(McpRecommendationQuery query);
}
