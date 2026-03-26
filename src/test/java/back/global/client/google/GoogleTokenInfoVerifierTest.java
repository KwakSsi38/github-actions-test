package back.global.client.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import back.domain.auth.port.GoogleIdTokenVerifier;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;

class GoogleTokenInfoVerifierTest {

    @Test
    void verify_success() throws Exception {
        String responseBody = """
                {
                  "sub": "google-sub-900",
                  "email": "user900@example.com",
                  "name": "User 900",
                  "aud": "test-client-id",
                  "iss": "https://accounts.google.com",
                  "email_verified": "true"
                }
                """;

        try (TokenInfoStubServer stubServer = TokenInfoStubServer.start(200, responseBody)) {
            GoogleTokenInfoVerifier verifier = new GoogleTokenInfoVerifier("test-client-id", stubServer.baseUrl());

            GoogleIdTokenVerifier.GoogleUserInfo verifiedUser = verifier.verify("id-token-900");

            assertThat(verifiedUser.googleSub()).isEqualTo("google-sub-900");
            assertThat(verifiedUser.email()).isEqualTo("user900@example.com");
            assertThat(verifiedUser.name()).isEqualTo("User 900");
        }
    }

    @Test
    void verify_whenAudienceMismatch() throws Exception {
        String responseBody = """
                {
                  "sub": "google-sub-901",
                  "email": "user901@example.com",
                  "name": "User 901",
                  "aud": "another-client-id",
                  "iss": "accounts.google.com",
                  "email_verified": true
                }
                """;

        try (TokenInfoStubServer stubServer = TokenInfoStubServer.start(200, responseBody)) {
            GoogleTokenInfoVerifier verifier = new GoogleTokenInfoVerifier("test-client-id", stubServer.baseUrl());

            assertThatThrownBy(() -> verifier.verify("id-token-901"))
                    .isInstanceOf(ServiceException.class)
                    .satisfies(exception -> {
                        ServiceException serviceException = (ServiceException) exception;
                        assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
                    });
        }
    }

    @Test
    void verify_whenTokenInfoStatusIsNot200() throws Exception {
        try (TokenInfoStubServer stubServer = TokenInfoStubServer.start(401, "{\"error\":\"invalid_token\"}")) {
            GoogleTokenInfoVerifier verifier = new GoogleTokenInfoVerifier("test-client-id", stubServer.baseUrl());

            assertThatThrownBy(() -> verifier.verify("id-token-902"))
                    .isInstanceOf(ServiceException.class)
                    .satisfies(exception -> {
                        ServiceException serviceException = (ServiceException) exception;
                        assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
                    });
        }
    }

    @Test
    void verify_whenClientIdNotConfigured() {
        GoogleTokenInfoVerifier verifier = new GoogleTokenInfoVerifier("", "http://localhost:18080");

        assertThatThrownBy(() -> verifier.verify("id-token-903"))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
                });
    }

    private static final class TokenInfoStubServer implements AutoCloseable {
        private final HttpServer httpServer;

        private TokenInfoStubServer(HttpServer httpServer) {
            this.httpServer = httpServer;
        }

        static TokenInfoStubServer start(int statusCode, String responseBody) throws IOException {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
            httpServer.createContext("/tokeninfo", exchange -> {
                byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(statusCode, bodyBytes.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(bodyBytes);
                }
            });
            httpServer.start();
            return new TokenInfoStubServer(httpServer);
        }

        String baseUrl() {
            return "http://localhost:" + httpServer.getAddress().getPort();
        }

        @Override
        public void close() {
            httpServer.stop(0);
        }
    }
}
