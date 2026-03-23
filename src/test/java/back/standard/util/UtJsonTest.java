package back.standard.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UtJsonTest {

    @Test
    @DisplayName("정상 객체는 JSON 문자열로 변환한다")
    void toString_success() {
        Map<String, Object> payload = Map.of("name", "start-ai-hub", "version", 1);

        String json = Ut.Json.toString(payload);

        assertThat(json).contains("\"name\":\"start-ai-hub\"");
        assertThat(json).contains("\"version\":1");
    }

    @Test
    @DisplayName("직렬화 실패 시 기본값을 반환한다")
    void toString_withDefaultValue() {
        Map<String, Object> recursive = new HashMap<>();
        recursive.put("self", recursive);

        String json = Ut.Json.toString(recursive, "{\"fallback\":true}");

        assertThat(json).isEqualTo("{\"fallback\":true}");
    }

    @Test
    @DisplayName("기본값 미지정 상태에서 직렬화 실패 시 null을 반환한다")
    void toString_withoutDefaultValue() {
        Map<String, Object> recursive = new HashMap<>();
        recursive.put("self", recursive);

        String json = Ut.Json.toString(recursive);

        assertThat(json).isNull();
    }
}
