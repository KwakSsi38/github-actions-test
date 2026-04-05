package back.domain.aimodel.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * OpenRouter /api/v1/models raw 응답 구조
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "생성자에서 List.copyOf를 통한 방어적 복사를 수행하여 내부 리스트의 불변성을 보장함"
)
public record OrModelsResponse(
        List<OrModel> data
) {
    public OrModelsResponse(List<OrModel> data) {
        this.data = data == null ? List.of() : List.copyOf(data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrModel(
            String id,
            String name,
            String description,

            @JsonProperty("context_length")
            Integer contextLength,

            Architecture architecture,
            Pricing pricing,

            @JsonProperty("top_provider")
            TopProvider topProvider
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Architecture(
                @JsonProperty("input_modalities")
                List<String> inputModalities,

                @JsonProperty("output_modalities")
                List<String> outputModalities
        ) {
            public Architecture(List<String> inputModalities, List<String> outputModalities) {
                this.inputModalities  = inputModalities  == null ? List.of() : List.copyOf(inputModalities);
                this.outputModalities = outputModalities == null ? List.of() : List.copyOf(outputModalities);
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Pricing(
                String prompt,
                String completion
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record TopProvider(
                @JsonProperty("max_completion_tokens")
                Integer maxCompletionTokens
        ) {}
    }
}