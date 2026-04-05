package back.domain.prompt.chunking.dto;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "WebClient 직렬화를 위한 요청 DTO로 입력 리스트를 그대로 유지한다."
)
public record EmbeddingRequest(
        List<String> texts
) {
}
