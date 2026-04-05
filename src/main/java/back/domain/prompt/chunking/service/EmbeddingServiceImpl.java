package back.domain.prompt.chunking.service;

import back.domain.prompt.chunking.dto.EmbeddingRequest;
import back.domain.prompt.chunking.dto.EmbeddingResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "공유 WebClient 빈은 스프링이 주입하고 관리한다."
)
public class EmbeddingServiceImpl implements EmbeddingService {

    private final WebClient embeddingWebClient;

    // 배치 사이즈는 Fast API에 있는 배치 사이즈와 통일시켜야 한다.
    private static final int BATCH_SIZE = 32;

    public List<Float> embed(String text) {
        EmbeddingResponse response = embeddingWebClient.post()
                .uri("/embed")
                .bodyValue(new EmbeddingRequest(List.of(text)))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("임베딩 응답이 비어 있습니다.");
        }

        return response.embeddings().getFirst();
    }

    // 배치로 묶어서 전송 (32개씩)
    public List<List<Float>> embedBatch(List<String> texts) {

        List<List<Float>> allEmbeddings = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));

            EmbeddingResponse response = embeddingWebClient.post()
                    .uri("/embed")
                    .bodyValue(new EmbeddingRequest(batch))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .block();

            if (response == null || response.embeddings() == null) {
                throw new IllegalStateException("임베딩 응답이 비어 있습니다. batchStart=" + i);
            }

            // 배치 크기와 반환된 임베딩 크기가 일치하는지 확인
            if (response.embeddings().size() != batch.size()) {
                throw new IllegalStateException("임베딩 응답의 크기가 요청된 배치 크기와 일치하지 않습니다. " +
                        "요청된 배치 크기: " + batch.size() + ", 반환된 임베딩 크기: " + response.embeddings().size() +
                        ", batchStart=" + i);
            }

            allEmbeddings.addAll(response.embeddings());
        }

        return allEmbeddings;
    }
}
