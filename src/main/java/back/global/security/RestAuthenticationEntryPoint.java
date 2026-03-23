package back.global.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import back.global.response.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    public static final String ERROR_CODE_ATTRIBUTE = "AUTH_ERROR_CODE";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "AUTH_ERROR_MESSAGE";

    private static final String DEFAULT_ERROR_CODE = "401-1";
    private static final String DEFAULT_ERROR_MESSAGE = "로그인이 필요합니다.";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String errorCode = readStringAttribute(request, ERROR_CODE_ATTRIBUTE, DEFAULT_ERROR_CODE);
        String errorMessage = readStringAttribute(request, ERROR_MESSAGE_ATTRIBUTE, DEFAULT_ERROR_MESSAGE);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new RsData<>(errorCode, errorMessage));
    }

    private String readStringAttribute(HttpServletRequest request, String name, String defaultValue) {
        Object value = request.getAttribute(name);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }
}
