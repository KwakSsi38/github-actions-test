package back.domain.mcp.recommendation.dto;

public record McpRecommendedSkillResponse(
        long skillId,
        String category,
        double finalScore,
        McpRecommendationScoreBreakdown scoreBreakdown,
        String sourceRepo,
        String skillMdRaw) {}
