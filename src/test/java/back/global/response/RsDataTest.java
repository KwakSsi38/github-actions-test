package back.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class RsDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("message-only 생성자는 data를 null로 초기화한다")
    void messageOnlyConstructor() {
        RsData<String> rsData = new RsData<>("생성 성공");

        assertThat(rsData.data()).isNull();
        assertThat(rsData.message()).isEqualTo("생성 성공");
    }

    @Test
    @DisplayName("JSON 직렬화 시 data/message 포맷을 유지한다")
    void serializeContract() throws Exception {
        RsData<Map<String, String>> rsData = new RsData<>(Map.of("id", "1"), "성공");

        String json = objectMapper.writeValueAsString(rsData);

        assertThat(json).contains("\"data\":{\"id\":\"1\"}");
        assertThat(json).contains("\"message\":\"성공\"");
    }
}
