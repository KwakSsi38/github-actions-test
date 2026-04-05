package back.domain.prompt.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SkillDtoTest {

    @Test
    @DisplayName("rawMetadata getter는 설정된 맵 값을 그대로 반환한다")
    void getRawMetadataReturnsAssignedValue() {
        SkillDto skillDto = new SkillDto();
        Map<String, Object> rawMetadata = new HashMap<>(Map.of("k", "v"));
        ReflectionTestUtils.setField(skillDto, "rawMetadata", rawMetadata);

        Map<String, Object> returned = skillDto.getRawMetadata();

        assertThat(returned).isSameAs(rawMetadata);
        assertThat(returned).containsEntry("k", "v");
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void getRawMetadataReturnsNullWhenNull() {
        SkillDto skillDto = new SkillDto();

        assertThat(skillDto.getRawMetadata()).isNull();
    }
}
