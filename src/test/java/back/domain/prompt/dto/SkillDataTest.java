package back.domain.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class SkillDataTest {

    @Test
    @DisplayName("rawMetadata getter는 방어적 복사본을 반환한다")
    void getRawMetadataReturnsDefensiveCopy() {
        SkillData skillData = new SkillData();
        ReflectionTestUtils.setField(skillData, "rawMetadata", Map.of("k", "v"));

        Map<String, Object> copied = skillData.getRawMetadata();

        assertThat(copied).containsEntry("k", "v");
        copied.put("newKey", "newValue");
        assertThat(skillData.getRawMetadata()).doesNotContainKey("newKey");
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void getRawMetadataReturnsNullWhenNull() {
        SkillData skillData = new SkillData();

        assertThat(skillData.getRawMetadata()).isNull();
    }
}
