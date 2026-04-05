package back.domain.info.dto.data;

import back.domain.info.enums.MetricType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ModelBenchmarkDto {

    @JsonProperty("aa_slug")
    private String modelApiId;

    @JsonProperty("metric_type")
    private MetricType metricType;

    @JsonProperty("metric_value")
    private BigDecimal metricValue;

    @JsonProperty("measured_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime measuredAt;

    @JsonProperty("unit")
    private String unit;
}
