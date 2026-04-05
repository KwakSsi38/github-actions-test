package back.domain.prompt.chunking.dto;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "임베딩 서버 응답 DTO로 수신한 리스트를 그대로 유지한다."
)
public record EmbeddingResponse(
        List<List<Float>> embeddings
) {
}
