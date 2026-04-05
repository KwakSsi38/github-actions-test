package back.domain.prompt.search.service;

import java.util.Comparator;
import java.util.stream.Collectors;

import back.domain.prompt.chunking.service.EmbeddingService;
import back.domain.prompt.prompt.enums.Category;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.dto.chunk.SkillChunkVectorSearchRowDto;
import back.domain.prompt.search.repository.SkillChunkVectorSearchRepository;
import back.domain.prompt.search.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillSearchServiceImpl implements SkillSearchService{

    private final EmbeddingService embeddingService;
    private final SkillChunkVectorSearchRepository skillChunkVectorSearchRepository;

    private static final int DEFAULT_TOP_K = 30;

    public SkillChunkSearchResultDto search(String query) {
        // query 값을 임베딩해서 float[] 형태의 벡터로 변환
        List<Float> queryEmbedding = embeddingService.embed(query);
        // 임베딩 벡터를 PostgreSQL pgvector 형식 문자열("[v1,v2,...]")로 변환
        String queryVector = VectorUtils.toPgVector(queryEmbedding);

        List<SkillChunkVectorSearchRowDto> results =
                skillChunkVectorSearchRepository.searchTopK(queryVector, DEFAULT_TOP_K * 3);

        List<CandidateDto> candidates = results.stream()
                .collect(Collectors.groupingBy(SkillChunkVectorSearchRowDto::getSkillId))
                .values().stream()
                .map(this::toGroupedCandidateDto)
                .sorted(Comparator.comparing(CandidateDto::primaryScore).reversed())
                .limit(DEFAULT_TOP_K)
                .toList();

        return new SkillChunkSearchResultDto(candidates);
    }

    // 후보들 중에 skills_id가 같은 chunk들은 하나로 묶고 제일 높은 점수(similarity)를 반환한다.
    private CandidateDto toGroupedCandidateDto(List<SkillChunkVectorSearchRowDto> groupedResults) {
        SkillChunkVectorSearchRowDto best = groupedResults.stream()
                .max(Comparator.comparing(SkillChunkVectorSearchRowDto::getSimilarity))
                .orElseThrow();

        return new CandidateDto(
                best.getSkillId(),
                best.getSkillName(),
                best.getRepositoryName(),
                best.getRepositoryUrl(),
                best.getContentMd(),
                Category.valueOf(best.getCategory()),
                best.getSummary() == null || best.getSummary().isBlank() ? null : best.getSummary(),
                best.getSimilarity(),
                new CandidateMetadataDto(
                        best.getStars(),
                        best.getForks(),
                        best.getUpdatedAt() != null ? best.getUpdatedAt().toString() : null
                )
        );
    }
}
