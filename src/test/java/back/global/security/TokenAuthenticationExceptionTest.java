package back.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.global.exception.CommonErrorCode;

class TokenAuthenticationExceptionTest {

    @Test
    @DisplayName("토큰 예외는 타입과 응답코드를 유지한다")
    void tokenAuthenticationException() {
        TokenAuthenticationException tokenAuthenticationException =
                new TokenAuthenticationException(
                        TokenAuthenticationException.TokenErrorType.EXPIRED,
                        "[TokenAuthenticationExceptionTest#tokenAuthenticationException] token expired",
                        "만료된 토큰입니다.");

        assertThat(tokenAuthenticationException.tokenErrorType())
                .isEqualTo(TokenAuthenticationException.TokenErrorType.EXPIRED);
        assertThat(tokenAuthenticationException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
        assertThat(tokenAuthenticationException.getRsData().message()).isEqualTo("만료된 토큰입니다.");
    }
}
