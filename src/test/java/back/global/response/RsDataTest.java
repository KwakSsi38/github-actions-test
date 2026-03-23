package back.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class RsDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("resultCode에서 statusCode를 파싱한다")
    void parseStatusCode() {
        RsData<String> rsData = new RsData<>("201-1", "생성 성공", "ok");

        assertThat(rsData.statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("JSON 직렬화 시 statusCode는 노출하지 않고 resultCode/msg/data를 유지한다")
    void serializeContract() throws Exception {
        RsData<Map<String, String>> rsData = new RsData<>("200-1", "성공", Map.of("id", "1"));

        String json = objectMapper.writeValueAsString(rsData);

        assertThat(json).contains("\"resultCode\":\"200-1\"");
        assertThat(json).contains("\"msg\":\"성공\"");
        assertThat(json).contains("\"data\":{\"id\":\"1\"}");
        assertThat(json).doesNotContain("statusCode");
    }
}
