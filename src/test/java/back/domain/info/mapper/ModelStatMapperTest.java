package back.domain.info.mapper;

import back.domain.info.dto.data.ModelBenchmarkDto;
import back.domain.info.enums.MetricType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ModelStatMapperTest {

    private final ModelStatMapper mapper = new ModelStatMapper();

    @Test
    void toModelBenchmarkEntity_mapsFields() {
        ModelBenchmarkDto dto = new ModelBenchmarkDto();
        LocalDateTime measuredAt = LocalDateTime.of(2026, 3, 27, 9, 30);

        ReflectionTestUtils.setField(dto, "modelApiId", "gpt-4.1");
        ReflectionTestUtils.setField(dto, "metricType", MetricType.CODING);
        ReflectionTestUtils.setField(dto, "metricValue", new BigDecimal("92.30"));
        ReflectionTestUtils.setField(dto, "measuredAt", measuredAt);
        ReflectionTestUtils.setField(dto, "unit", "score");

        var entity = mapper.toModelBenchmarkEntity(dto);

        assertThat(entity.getModelApiId()).isEqualTo("gpt-4.1");
        assertThat(entity.getMetricType()).isEqualTo(MetricType.CODING);
        assertThat(entity.getMetricValue()).isEqualByComparingTo("92.30");
        assertThat(entity.getMeasuredAt()).isEqualTo(measuredAt);
        assertThat(entity.getUnit()).isEqualTo("score");
    }
}
