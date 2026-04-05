package back.domain.aimodel.dto.artificialanalysis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

/**
 * Artificial Analysis /api/v1/data/llms/models raw 응답 구조
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "생성자에서 List.copyOf를 통한 방어적 복사를 수행하여 내부 리스트의 불변성을 보장함"
)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AaModelsResponse(
        List<AaModel> data
) {
    public AaModelsResponse(List<AaModel> data) {
        this.data = data == null ? List.of() : List.copyOf(data);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AaModel(
            String slug,
            String name,

            @JsonProperty("release_date")
            String releaseDate,

            @JsonProperty("context_length")
            Integer contextLength,

            @JsonProperty("model_creator")
            ModelCreator modelCreator,

            Pricing pricing,
            Evaluations evaluations,

            @JsonProperty("median_output_tokens_per_second")
            Double medianOutputTokensPerSecond,

            @JsonProperty("median_time_to_first_token_seconds")
            Double medianTimeToFirstTokenSeconds
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ModelCreator(
                String name,
                String slug
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Pricing(
                @JsonProperty("price_1m_input_tokens")
                Double price1mInputTokens,

                @JsonProperty("price_1m_output_tokens")
                Double price1mOutputTokens,

                @JsonProperty("price_1m_blended_3_to_1")
                Double price1mBlended3to1
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Evaluations(
                @JsonProperty("artificial_analysis_intelligence_index")
                Double intelligenceIndex,

                @JsonProperty("artificial_analysis_coding_index")
                Double codingIndex,

                @JsonProperty("artificial_analysis_math_index")
                Double mathIndex
        ) {}
    }
}