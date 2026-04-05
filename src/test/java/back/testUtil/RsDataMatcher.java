package back.testUtil;

import back.global.exception.ErrorCode;
import back.global.response.RsData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MockMvc 테스트에서 {@link RsData} 응답을 검증하는 커스텀 ResultMatcher.
 *
 * 여러 ResultMatcher를 조합해 응답 전체를 한 번에 검증합니다.
 * data 필드의 단순 타입, 중첩 객체, 리스트, Java Time 타입을 재귀적으로 처리합니다.
 *
 * <pre>
 * // message만 검증
 * mockMvc.perform(...)
 *        .andExpect(RsDataMatcher.hasMessage("성공했습니다."));
 *
 * // data + message 전체 검증
 * mockMvc.perform(...)
 *        .andExpect(RsDataMatcher.of(new RsData<>(responseDto, "성공했습니다.")));
 *
 * // 에러 응답 검증
 * mockMvc.perform(...)
 *        .andExpect(RsDataMatcher.hasError(CommonErrorCode.FORBIDDEN));
 * </pre>
 */
public class RsDataMatcher implements ResultMatcher {

    private final List<ResultMatcher> matchers;

    private RsDataMatcher(List<ResultMatcher> matchers) {
        this.matchers = matchers;
    }

    @Override
    public void match(MvcResult result) throws Exception {
        for (ResultMatcher matcher : matchers) {
            matcher.match(result);
        }
    }

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────

    /**
     * RsData 응답의 message 필드만 검증합니다.
     */
    public static ResultMatcher hasMessage(String message) {
        return MockMvcResultMatchers.jsonPath("$.message").value(message);
    }

    /**
     * ErrorCode 기준으로 HTTP 상태코드와 message를 검증합니다.
     */
    public static ResultMatcher hasError(ErrorCode errorCode) {
        List<ResultMatcher> matchers = new ArrayList<>();
        matchers.add(MockMvcResultMatchers.status().is(errorCode.statusCode()));
        matchers.add(MockMvcResultMatchers.jsonPath("$.message").value(errorCode.defaultMessage()));
        return new RsDataMatcher(matchers);
    }

    /**
     * RsData 전체(data + message)를 검증합니다.
     * data가 null이면 null 여부만 검증합니다.
     */
    public static <T> ResultMatcher of(RsData<T> rsData) {
        List<ResultMatcher> matchers = new ArrayList<>();

        if (rsData.message() != null) {
            matchers.add(MockMvcResultMatchers.jsonPath("$.message").value(rsData.message()));
        }

        T data = rsData.data();
        if (data == null) {
            matchers.add(MockMvcResultMatchers.jsonPath("$.data").doesNotExist());
        } else {
            matchers.addAll(buildMatchers("$.data", data));
        }

        return new RsDataMatcher(matchers);
    }

    /**
     * 특정 JSON 경로의 값을 직접 검증합니다.
     */
    public static ResultMatcher hasField(String path, Object value) {
        return MockMvcResultMatchers.jsonPath("$." + path).value(value);
    }

    // ── 내부 구현 ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<ResultMatcher> buildMatchers(String path, Object value) {
        List<ResultMatcher> matchers = new ArrayList<>();

        if (value == null) {
            matchers.add(MockMvcResultMatchers.jsonPath(path).doesNotExist());
        } else if (isSimple(value)) {
            matchers.add(MockMvcResultMatchers.jsonPath(path).value(value));
        } else if (value instanceof LocalDate date) {
            matchers.add(MockMvcResultMatchers.jsonPath(path)
                    .value(date.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        } else if (value instanceof LocalTime time) {
            matchers.add(MockMvcResultMatchers.jsonPath(path)
                    .value(time.format(DateTimeFormatter.ISO_LOCAL_TIME)));
        } else if (value instanceof LocalDateTime dateTime) {
            matchers.add(MockMvcResultMatchers.jsonPath(path)
                    .value(dateTime.truncatedTo(ChronoUnit.SECONDS)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))));
        } else if (value instanceof List<?> list) {
            matchers.add(MockMvcResultMatchers.jsonPath(path + ".length()").value(list.size()));
            for (int i = 0; i < list.size(); i++) {
                matchers.addAll(buildMatchers(path + "[" + i + "]", list.get(i)));
            }
        } else {
            Map<String, Object> map = createMapper().convertValue(value, Map.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                matchers.addAll(buildMatchers(path + "." + entry.getKey(), entry.getValue()));
            }
        }

        return matchers;
    }

    private static boolean isSimple(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
