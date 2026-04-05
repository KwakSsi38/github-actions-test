package back.domain.prompt.chunking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

class EmbeddingServiceImplTest {

    @Test
    @DisplayName("단일 임베딩 요청 시 첫 번째 벡터를 반환한다")
    void embed_returnsFirstEmbedding() {
        EmbeddingServiceImpl embeddingService = new EmbeddingServiceImpl(webClientResponding(
                jsonResponse("{\"embeddings\":[[0.1,0.2]]}")
        ));

        List<Float> embedding = embeddingService.embed("hello");

        assertThat(embedding).containsExactly(0.1f, 0.2f);
    }

    @Test
    @DisplayName("단일 임베딩 응답이 비어 있으면 예외를 던진다")
    void embed_throwsWhenResponseIsEmpty() {
        EmbeddingServiceImpl embeddingService = new EmbeddingServiceImpl(webClientResponding(
                jsonResponse("{\"embeddings\":[]}")
        ));

        assertThatThrownBy(() -> embeddingService.embed("hello"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("배치 임베딩은 32개 단위로 나눠 여러 응답을 합친다")
    void embedBatch_splitsIntoFixedSizeBatchesAndAggregatesResponses() {
        AtomicInteger callCount = new AtomicInteger();
        EmbeddingServiceImpl embeddingService = new EmbeddingServiceImpl(WebClient.builder()
                .exchangeFunction(request -> {
                    int invocation = callCount.getAndIncrement();
                    String body = invocation == 0
                            ? jsonForEmbeddings(0, 32)
                            : jsonForEmbeddings(32, 1);
                    return Mono.just(jsonResponse(body));
                })
                .build());

        List<List<Float>> embeddings = embeddingService.embedBatch(
                java.util.stream.IntStream.range(0, 33)
                        .mapToObj(i -> "text-" + i)
                        .toList()
        );

        assertThat(callCount.get()).isEqualTo(2);
        assertThat(embeddings).hasSize(33);
        assertThat(embeddings.getFirst()).containsExactly(0.0f);
        assertThat(embeddings.get(32)).containsExactly(32.0f);
    }

    @Test
    @DisplayName("배치 임베딩 응답에 embeddings 필드가 없으면 예외를 던진다")
    void embedBatch_throwsWhenEmbeddingsAreMissing() {
        EmbeddingServiceImpl embeddingService = new EmbeddingServiceImpl(webClientResponding(
                jsonResponse("{}")
        ));

        assertThatThrownBy(() -> embeddingService.embedBatch(List.of("hello")))
                .isInstanceOf(IllegalStateException.class);
    }

    private WebClient webClientResponding(ClientResponse response) {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.just(response))
                .build();
    }

    private ClientResponse jsonResponse(String body) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }

    private String jsonForEmbeddings(int startInclusive, int count) {
        String embeddings = java.util.stream.IntStream.range(startInclusive, startInclusive + count)
                .mapToObj(value -> "[" + value + ".0]")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"embeddings\":[" + embeddings + "]}";
    }
}
