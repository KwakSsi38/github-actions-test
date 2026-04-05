package back.domain.info.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import back.domain.info.dto.data.CategoryStatDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CategoryStatDtoTest {

    @Test
    @DisplayName("DTO getter가 주입된 값을 그대로 반환한다")
    void getters_returnAssignedValues() {
        CategoryStatDto dto = new CategoryStatDto();
        LocalDateTime lastUpdated = LocalDateTime.of(2026, 3, 27, 11, 15);

        ReflectionTestUtils.setField(dto, "category", "coding");
        ReflectionTestUtils.setField(dto, "avgValue", new BigDecimal("87.30"));
        ReflectionTestUtils.setField(dto, "maxValue", new BigDecimal("99.10"));
        ReflectionTestUtils.setField(dto, "minValue", new BigDecimal("65.40"));
        ReflectionTestUtils.setField(dto, "sampleCount", 24);
        ReflectionTestUtils.setField(dto, "lastUpdated", lastUpdated);

        assertThat(dto.getCategory()).isEqualTo("coding");
        assertThat(dto.getAvgValue()).isEqualByComparingTo("87.30");
        assertThat(dto.getMaxValue()).isEqualByComparingTo("99.10");
        assertThat(dto.getMinValue()).isEqualByComparingTo("65.40");
        assertThat(dto.getSampleCount()).isEqualTo(24);
        assertThat(dto.getLastUpdated()).isEqualTo(lastUpdated);
    }
}
