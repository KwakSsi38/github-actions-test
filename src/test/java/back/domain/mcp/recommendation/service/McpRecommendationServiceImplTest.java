package back.domain.mcp.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.domain.mcp.candidate.provider.McpRecommendationCandidateProvider;
import back.domain.mcp.recommendation.dto.McpRecommendationRequest;
import back.domain.mcp.recommendation.dto.McpRecommendationResponse;
import back.domain.mcp.recommendation.dto.McpRecommendationScoreBreakdown;
import back.domain.mcp.recommendation.dto.McpRecommendedSkillResponse;
import back.domain.mcp.recommendation.ranker.McpRecommendationRanker;

@ExtendWith(MockitoExtension.class)
class McpRecommendationServiceImplTest {

    @Mock
    private McpRecommendationCandidateProvider mcpRecommendationCandidateProvider;

    @Mock
    private McpRecommendationRanker mcpRecommendationRanker;

    private McpRecommendationService mcpRecommendationService;

    @BeforeEach
    void setUp() {
        mcpRecommendationService =
                new McpRecommendationServiceImpl(mcpRecommendationCandidateProvider, mcpRecommendationRanker);
    }

    @Test
    @DisplayName("추천 요청 시 keywords를 정규화한 query 문자열로 후보 조회를 호출한다")
    void recommend_normalizeQueryAndReturnRankedResult() {
        McpRecommendationRequest request = new McpRecommendationRequest(" SpringBoot   infra  DevOps ");
        List<McpRecommendationCandidate> candidates = List.of(new McpRecommendationCandidate(
                1L,
                "spring-infra-skill",
                "repo-name",
                "https://github.com/example/repo-name",
                "# skill content",
                "INFRA",
                "summary",
                0.91,
                null));
        List<McpRecommendedSkillResponse> rankedSkills = List.of(new McpRecommendedSkillResponse(
                1L,
                "INFRA",
                0.88,
                new McpRecommendationScoreBreakdown(0.91, 0.8, 0.7, 0.9),
                "example/repo-name",
                "# skill content"));

        when(mcpRecommendationCandidateProvider.findTopCandidates(any(McpRecommendationQuery.class)))
                .thenReturn(candidates);
        when(mcpRecommendationRanker.rank(candidates)).thenReturn(rankedSkills);

        McpRecommendationResponse response = mcpRecommendationService.recommend(request);

        ArgumentCaptor<McpRecommendationQuery> queryCaptor = ArgumentCaptor.forClass(McpRecommendationQuery.class);
        verify(mcpRecommendationCandidateProvider).findTopCandidates(queryCaptor.capture());
        assertThat(queryCaptor.getValue().query()).isEqualTo("SpringBoot infra DevOps");
        assertThat(response.selectedSkills()).hasSize(1);
        assertThat(response.selectedSkills().getFirst().category()).isEqualTo("INFRA");
    }
}
