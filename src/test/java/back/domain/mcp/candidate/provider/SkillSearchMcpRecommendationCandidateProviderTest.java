package back.domain.mcp.candidate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;

@ExtendWith(MockitoExtension.class)
class SkillSearchMcpRecommendationCandidateProviderTest {

    @Mock
    private SkillSearchService skillSearchService;

    private SkillSearchMcpRecommendationCandidateProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SkillSearchMcpRecommendationCandidateProvider(skillSearchService);
    }

    @Test
    @DisplayName("스킬 검색 응답을 추천 후보 DTO로 매핑한다")
    void findTopCandidates_mapsSearchResult() {
        CandidateDto candidate = new CandidateDto(
                10L,
                "spring-backend-skill",
                "spring-repo",
                "https://github.com/example/spring-repo",
                "# content",
                Category.BACKEND,
                "summary",
                0.83f,
                new CandidateMetadataDto(210, 45, "2026-02-10T09:10:00Z"));
        when(skillSearchService.search("SpringBoot infra DevOps"))
                .thenReturn(new SkillChunkSearchResultDto(List.of(candidate)));

        List<McpRecommendationCandidate> result =
                provider.findTopCandidates(new McpRecommendationQuery("SpringBoot infra DevOps"));

        assertThat(result).hasSize(1);
        McpRecommendationCandidate mapped = result.getFirst();
        assertThat(mapped.skillId()).isEqualTo(10L);
        assertThat(mapped.category()).isEqualTo("BACKEND");
        assertThat(mapped.primaryScore()).isCloseTo(0.83, within(0.000001));
        assertThat(mapped.metadata()).isNotNull();
        assertThat(mapped.metadata().stars()).isEqualTo(210);
    }

    @Test
    @DisplayName("metadata가 없으면 null로 매핑한다")
    void findTopCandidates_whenMetadataIsNull_mapsNull() {
        CandidateDto candidate = new CandidateDto(
                11L,
                "infra-skill",
                "infra-repo",
                "https://github.com/example/infra-repo",
                "# infra",
                Category.INFRA,
                null,
                0.7f,
                null);
        when(skillSearchService.search("infra"))
                .thenReturn(new SkillChunkSearchResultDto(List.of(candidate)));

        List<McpRecommendationCandidate> result =
                provider.findTopCandidates(new McpRecommendationQuery("infra"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().metadata()).isNull();
    }
}
