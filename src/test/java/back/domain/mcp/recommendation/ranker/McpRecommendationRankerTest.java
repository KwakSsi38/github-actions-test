package back.domain.mcp.recommendation.ranker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationCandidateMetadata;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;

class McpRecommendationRankerTest {

    private final McpRecommendationRanker mcpRecommendationRanker = new McpRecommendationRankerImpl();

    @Test
    @DisplayName("임계치 미만 후보는 제외하고 카테고리별 최고 점수 1개를 선택한다")
    void rank_filtersByThresholdAndDedupByCategory() {
        List<McpRecommendationCandidate> candidates = List.of(
                candidate(1L, "backend-high", "backend", 0.90, 200, 40, OffsetDateTime.now().minusDays(1).toString()),
                candidate(2L, "backend-low", "backend", 0.55, 20, 3, OffsetDateTime.now().minusDays(20).toString()),
                candidate(3L, "infra-high", "infra", 0.84, 120, 15, OffsetDateTime.now().minusDays(2).toString()),
                candidate(4L, "uncategorized", null, 0.82, 80, 12, OffsetDateTime.now().minusDays(3).toString()),
                candidate(5L, "below-threshold", "devops", 0.10, 0, 0, OffsetDateTime.now().minusDays(500).toString()));

        List<McpRecommendedSkillResponse> selected = mcpRecommendationRanker.rank(candidates);

        assertThat(selected).extracting(McpRecommendedSkillResponse::skillId).contains(1L, 3L, 4L);
        assertThat(selected).extracting(McpRecommendedSkillResponse::skillId).doesNotContain(2L, 5L);
        assertThat(selected).extracting(McpRecommendedSkillResponse::category).contains("backend", "infra", "uncategorized");
    }

    @Test
    @DisplayName("카테고리 dedup 이후 최종 결과는 최대 10개만 반환한다")
    void rank_limitsToTop10() {
        List<McpRecommendationCandidate> candidates = new ArrayList<>();
        for (int index = 1; index <= 12; index++) {
            candidates.add(candidate(
                    (long) index,
                    "skill-%d".formatted(index),
                    "category-%d".formatted(index),
                    0.90 - (index * 0.01),
                    300 - (index * 5),
                    100 - index,
                    OffsetDateTime.now().minusDays(index).toString()));
        }

        List<McpRecommendedSkillResponse> selected = mcpRecommendationRanker.rank(candidates);

        assertThat(selected).hasSize(10);
    }

    private McpRecommendationCandidate candidate(
            long skillId,
            String skillName,
            String category,
            double primaryScore,
            int stars,
            int forks,
            String updatedAt) {
        return new McpRecommendationCandidate(
                skillId,
                skillName,
                "repo-%d".formatted(skillId),
                "https://github.com/example/repo-%d".formatted(skillId),
                "# skill content",
                category,
                "summary",
                primaryScore,
                new McpRecommendationCandidateMetadata(stars, forks, updatedAt));
    }
}
