package back.domain.mcp.recommendation.service;

import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;

public interface McpRecommendationService {
    McpRecommendationResponse recommend(McpRecommendationRequest request);
}
