package back.domain.mcp.candidate.provider;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import back.domain.mcp.candidate.dto.McpRecommendationCandidate;
import back.domain.mcp.candidate.dto.McpRecommendationCandidateMetadata;
import back.domain.mcp.candidate.dto.McpRecommendationQuery;
import back.domain.prompt.search.dto.candidate.CandidateDto;
import back.domain.prompt.search.dto.candidate.CandidateMetadataDto;
import back.domain.prompt.search.dto.chunk.SkillChunkSearchResultDto;
import back.domain.prompt.search.service.SkillSearchService;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.mcp.recommendation.candidate-source", havingValue = "skill-search")
@RequiredArgsConstructor
public class SkillSearchMcpRecommendationCandidateProvider implements McpRecommendationCandidateProvider {

    private final SkillSearchService skillSearchService;

    @Override
    public List<McpRecommendationCandidate> findTopCandidates(McpRecommendationQuery query) {
        SkillChunkSearchResultDto response = skillSearchService.search(query.query());
        return response.getCandidates().stream()
                .map(this::toMcpRecommendationCandidate)
                .toList();
    }

    private McpRecommendationCandidate toMcpRecommendationCandidate(CandidateDto candidate) {
        return new McpRecommendationCandidate(
                candidate.skillId(),
                candidate.skillName(),
                candidate.repositoryName(),
                candidate.repositoryUrl(),
                candidate.contentMd(),
                candidate.category() != null ? candidate.category().name() : null,
                candidate.summary(),
                (double) candidate.primaryScore(),
                toMetadata(candidate.metadata()));
    }

    private McpRecommendationCandidateMetadata toMetadata(CandidateMetadataDto metadata) {
        if (metadata == null) {
            return null;
        }

        return new McpRecommendationCandidateMetadata(
                metadata.getStars(),
                metadata.getForks(),
                metadata.getUpdatedAt());
    }
}
