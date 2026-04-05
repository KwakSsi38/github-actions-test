package back.domain.mcp.candidate.dto;

public record McpRecommendationQuery(String query) {
    public McpRecommendationQuery {
        query = normalize(query);
    }

    private static String normalize(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }

        return rawQuery.trim().replaceAll("\\s+", " ");
    }
}
