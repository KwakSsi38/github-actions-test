package back.domain.mcp.candidate.dto;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "응답 DTO로 읽기 전용 List.copyOf를 사용해 외부 변경을 방지합니다.")
public record McpRecommendationCandidatesResponse(List<McpRecommendationCandidate> candidates) {
    public McpRecommendationCandidatesResponse {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
