package back.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    @DisplayName("ErrorCode default statusCode는 HttpStatus 값을 반환한다")
    void statusCode() {
        assertThat(CommonErrorCode.BAD_REQUEST.statusCode()).isEqualTo(400);
        assertThat(CommonErrorCode.UNAUTHORIZED.statusCode()).isEqualTo(401);
    }
}
