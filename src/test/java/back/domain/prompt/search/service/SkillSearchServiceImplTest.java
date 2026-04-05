package back.domain.prompt.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import back.domain.prompt.search.repository.SkillChunkVectorSearchRepository;

@ExtendWith(MockitoExtension.class)
class SkillSearchServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SkillChunkVectorSearchRepository skillChunkVectorSearchRepository;

    @Test
    @DisplayName("search는 chunk 결과를 skill 기준으로 묶고 점수 순으로 반환한다")
    void search_groupsAndSortsCandidates() {
        SkillSearchServiceImpl skillSearchService =
                new SkillSearchServiceImpl(embeddingService, skillChunkVectorSearchRepository);

        when(embeddingService.embed("spring search")).thenReturn(List.of(0.1f, 0.2f));
        when(skillChunkVectorSearchRepository.searchTopK("[0.1,0.2]", 90)).thenReturn(List.of(
                row(11L, 1L, "alpha", "BACKEND", "   ", 0.82f, null),
                row(12L, 1L, "alpha", "BACKEND", "ignored", 0.71f, LocalDateTime.parse("2026-03-31T00:00:00")),
                row(21L, 2L, "beta", "FRONTEND", "frontend summary", 0.61f,
                        LocalDateTime.parse("2026-04-01T00:00:00"))
        ));

        SkillChunkSearchResultDto result = skillSearchService.search("spring search");

        verify(skillChunkVectorSearchRepository).searchTopK("[0.1,0.2]", 90);
        assertThat(result.getCandidates()).hasSize(2);

        assertThat(result.getCandidates().get(0).skillId()).isEqualTo(1L);
        assertThat(result.getCandidates().get(0).category()).isEqualTo(Category.BACKEND);
        assertThat(result.getCandidates().get(0).summary()).isNull();
        assertThat(result.getCandidates().get(0).primaryScore()).isEqualTo(0.82f);

        assertThat(result.getCandidates().get(1).skillId()).isEqualTo(2L);
        assertThat(result.getCandidates().get(1).category()).isEqualTo(Category.FRONTEND);
        assertThat(result.getCandidates().get(1).summary()).isEqualTo("frontend summary");
        assertThat(result.getCandidates().get(1).metadata().getUpdatedAt())
                .isEqualTo("2026-04-01T00:00");
    }

    private SkillChunkVectorSearchRowDto row(
            Long chunkId,
            Long skillId,
            String skillName,
            String category,
            String summary,
            float similarity,
            LocalDateTime updatedAt
    ) {
        return new SkillChunkVectorSearchRowDto(
                chunkId,
                skillId,
                skillName,
                "demo-repo",
                "https://example.com/" + skillName,
                skillName + " content",
                category,
                summary,
                10,
                2,
                updatedAt,
                similarity
        );
    }
}
