package back.domain.prompt.chunking.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChunkingDtoTest {

    @Test
    @DisplayName("chunking DTO record는 전달받은 값을 그대로 노출한다")
    void records_exposeProvidedValues() {
        EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of("alpha", "beta"));
        EmbeddingResponse embeddingResponse = new EmbeddingResponse(List.of(List.of(0.1f, 0.2f)));
        Section section = new Section("Install", "body");

        assertThat(embeddingRequest.texts()).containsExactly("alpha", "beta");
        assertThat(embeddingResponse.embeddings()).containsExactly(List.of(0.1f, 0.2f));
        assertThat(section.sectionTitle()).isEqualTo("Install");
        assertThat(section.text()).isEqualTo("body");
    }
}
