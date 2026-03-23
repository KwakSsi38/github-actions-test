package back.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import back.global.exception.ServiceException;

class BearerTokenResolverTest {

    private final BearerTokenResolver bearerTokenResolver = new BearerTokenResolver();

    @Test
    @DisplayName("Bearer 헤더에서 access token을 추출한다")
    void resolve() {
        String resolvedToken = bearerTokenResolver.resolve("Bearer access-token-value");

        assertThat(resolvedToken).isEqualTo("access-token-value");
    }

    @Test
    @DisplayName("헤더가 없으면 401-1 예외를 던진다")
    void resolve_whenHeaderMissing() {
        assertThatThrownBy(() -> bearerTokenResolver.resolve(null))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getRsData().resultCode()).isEqualTo("401-1");
                });
    }

    @Test
    @DisplayName("Bearer prefix가 아니면 401-1 예외를 던진다")
    void resolve_whenPrefixInvalid() {
        assertThatThrownBy(() -> bearerTokenResolver.resolve("Token access-token-value"))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getRsData().resultCode()).isEqualTo("401-1");
                });
    }

    @Test
    @DisplayName("토큰이 비어 있으면 401-1 예외를 던진다")
    void resolve_whenTokenBlank() {
        assertThatThrownBy(() -> bearerTokenResolver.resolve("Bearer   "))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getRsData().resultCode()).isEqualTo("401-1");
                });
    }
}
