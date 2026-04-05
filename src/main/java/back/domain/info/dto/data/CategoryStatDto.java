package back.domain.info.dto.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CategoryStatDto {

    @JsonProperty("category")
    private String category;

    @JsonProperty("avg_value")
    private BigDecimal avgValue;

    @JsonProperty("max_value")
    private BigDecimal maxValue;

    @JsonProperty("min_value")
    private BigDecimal minValue;

    @JsonProperty("sample_count")
    private Integer sampleCount;

    @JsonProperty("last_updated")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdated;
}
