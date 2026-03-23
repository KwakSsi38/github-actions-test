package back.global.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerWebMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("NoSuchElementException은 404 상태와 기본 메시지로 매핑한다")
    void handleNoSuchElement() throws Exception {
        mockMvc.perform(get("/_test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("ServiceException(ErrorCode)은 코드에 맞는 상태와 clientMessage를 반환한다")
    void handleServiceExceptionWithErrorCode() throws Exception {
        mockMvc.perform(get("/_test/service/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("회원이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException은 400 상태와 검증 메시지를 반환한다")
    void handleValidationException() throws Exception {
        mockMvc.perform(post("/_test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name-NotBlank-이름은 필수입니다."));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 상태와 기본 메시지로 변환한다")
    void handleUnexpectedException() throws Exception {
        mockMvc.perform(get("/_test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    @RestController
    static class TestController {

        @GetMapping("/_test/not-found")
        String notFound() {
            throw new NoSuchElementException("missing");
        }

        @GetMapping("/_test/service/not-found")
        String serviceNotFound() {
            throw new ServiceException(
                    CommonErrorCode.NOT_FOUND,
                    "[GlobalExceptionHandlerWebMvcTest#serviceNotFound] member not found",
                    "회원이 존재하지 않습니다.");
        }

        @PostMapping("/_test/validation")
        String validation(@Valid @RequestBody ValidationRequest request) {
            return request.name();
        }

        @GetMapping("/_test/unexpected")
        String unexpected() {
            throw new RuntimeException("boom");
        }
    }

    record ValidationRequest(@NotBlank(message = "name-NotBlank-이름은 필수입니다.") String name) {}
}
