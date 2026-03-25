package back.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Test
    @DisplayName("AccessToken 생성 후 payload를 추출할 수 있다")
    void accessTokenRoundTrip() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 7200);

        String accessToken = jwtTokenProvider.generateAccessToken(1L, "user@example.com", "USER");
        JwtTokenProvider.AccessTokenPayload payload = jwtTokenProvider.getAccessTokenPayload(accessToken);

        assertThat(payload.memberId()).isEqualTo(1L);
        assertThat(payload.role()).isEqualTo("USER");
        assertThat(jwtTokenProvider.getMemberIdFromAccessToken(accessToken)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getMemberRoleFromAccessToken(accessToken)).isEqualTo("USER");
    }

    @Test
    @DisplayName("RefreshToken에서 memberId를 추출할 수 있다")
    void refreshTokenRoundTrip() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 7200);

        String refreshToken = jwtTokenProvider.generateRefreshToken(5L, "user@example.com", "ADMIN");

        assertThat(jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken)).isEqualTo(5L);
    }

    @Test
    @DisplayName("RefreshToken을 AccessToken으로 파싱하면 INVALID 예외를 던진다")
    void accessTokenPayload_whenTokenTypeInvalid() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 7200);
        String refreshToken = jwtTokenProvider.generateRefreshToken(9L, "user@example.com", "USER");

        assertThatThrownBy(() -> jwtTokenProvider.getAccessTokenPayload(refreshToken))
                .isInstanceOf(TokenAuthenticationException.class)
                .satisfies(exception -> {
                    TokenAuthenticationException tokenException = (TokenAuthenticationException) exception;
                    assertThat(tokenException.tokenErrorType())
                            .isEqualTo(TokenAuthenticationException.TokenErrorType.INVALID);
                });
    }

    @Test
    @DisplayName("만료된 토큰은 EXPIRED 예외를 던진다")
    void accessTokenPayload_whenTokenExpired() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, -1, 7200);
        String accessToken = jwtTokenProvider.generateAccessToken(9L, "user@example.com", "USER");

        assertThatThrownBy(() -> jwtTokenProvider.getAccessTokenPayload(accessToken))
                .isInstanceOf(TokenAuthenticationException.class)
                .satisfies(exception -> {
                    TokenAuthenticationException tokenException = (TokenAuthenticationException) exception;
                    assertThat(tokenException.tokenErrorType())
                            .isEqualTo(TokenAuthenticationException.TokenErrorType.EXPIRED);
                });
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 INVALID 예외를 던진다")
    void accessTokenPayload_whenTokenMalformed() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 3600, 7200);

        assertThatThrownBy(() -> jwtTokenProvider.getAccessTokenPayload("invalid-token"))
                .isInstanceOf(TokenAuthenticationException.class)
                .satisfies(exception -> {
                    TokenAuthenticationException tokenException = (TokenAuthenticationException) exception;
                    assertThat(tokenException.tokenErrorType())
                            .isEqualTo(TokenAuthenticationException.TokenErrorType.INVALID);
                });
    }
}
