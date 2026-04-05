package back.domain.mcp.candidate.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class McpRecommendationQueryTest {

    @Test
    @DisplayName("query는 null 이면 빈 문자열로 정규화한다")
    void normalize_whenNull() {
        McpRecommendationQuery query = new McpRecommendationQuery(null);

        assertThat(query.query()).isEmpty();
    }

    @Test
    @DisplayName("query는 trim 후 다중 공백을 단일 공백으로 정규화한다")
    void normalize_whenMultipleSpaces() {
        McpRecommendationQuery query = new McpRecommendationQuery(" SpringBoot   infra  DevOps ");

        assertThat(query.query()).isEqualTo("SpringBoot infra DevOps");
    }
}
