package back.domain.prompt.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class AgentDataTest {

    @Test
    @DisplayName("rawMetadata getter는 방어적 복사본을 반환한다")
    void getRawMetadataReturnsDefensiveCopy() {
        AgentData agentData = new AgentData();
        ReflectionTestUtils.setField(agentData, "rawMetadata", Map.of("a", 1));

        Map<String, Object> copied = agentData.getRawMetadata();

        assertThat(copied).containsEntry("a", 1);
        copied.put("b", 2);
        assertThat(agentData.getRawMetadata()).doesNotContainKey("b");
    }

    @Test
    @DisplayName("rawMetadata가 null이면 null을 반환한다")
    void getRawMetadataReturnsNullWhenNull() {
        AgentData agentData = new AgentData();

        assertThat(agentData.getRawMetadata()).isNull();
    }
}
