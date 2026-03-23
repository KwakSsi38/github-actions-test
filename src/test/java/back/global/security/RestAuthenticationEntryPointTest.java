package back.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("에러 속성이 없으면 기본 401 응답을 반환한다")
    void commence_withDefaultValues() throws Exception {
        RestAuthenticationEntryPoint restAuthenticationEntryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        restAuthenticationEntryPoint.commence(
                request, response, new InsufficientAuthenticationException("unauthorized"));

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(jsonNode.get("data").isNull()).isTrue();
        assertThat(jsonNode.get("message").asText()).isEqualTo("로그인이 필요합니다.");
    }

    @Test
    @DisplayName("요청 속성에 지정한 메시지를 응답에 반영한다")
    void commence_withRequestAttributes() throws Exception {
        RestAuthenticationEntryPoint restAuthenticationEntryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(RestAuthenticationEntryPoint.ERROR_MESSAGE_ATTRIBUTE, "커스텀 인증 오류");

        restAuthenticationEntryPoint.commence(
                request, response, new InsufficientAuthenticationException("unauthorized"));

        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(jsonNode.get("data").isNull()).isTrue();
        assertThat(jsonNode.get("message").asText()).isEqualTo("커스텀 인증 오류");
    }
}
