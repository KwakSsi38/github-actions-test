package back.domain.mcp.recommendation.dto;

public record McpRecommendationScoreBreakdown(
        double primaryScore,
        double starsNorm,
        double forksNorm,
        double freshnessNorm) {}
