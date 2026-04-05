package back.domain.aimodel.dto.integrated;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * model_benchmarks_records.json 출력 구조
 *
 * model_api_id 대신 aa_slug 사용.
 * ai_models 테이블이 제거됨에 따라 AA slug 기준으로 레코드를 식별한다.
 */
public record BenchmarkRecord(
        @JsonProperty("aa_slug")
        String aaSlug,

        @JsonProperty("metric_type")
        String metricType,

        @JsonProperty("metric_value")
        double metricValue,

        @JsonProperty("measured_at")
        String measuredAt,

        String unit
) {}
