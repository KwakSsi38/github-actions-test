package back.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.security.TokenAuthenticationException.TokenErrorType;
import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BearerTokenResolver bearerTokenResolver;

    @Mock
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 다음 필터로 진행한다")
    void doFilterInternal_withoutAuthorizationHeader() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, bearerTokenResolver, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보를 세팅한다")
    void doFilterInternal_withValidToken() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, bearerTokenResolver, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        when(bearerTokenResolver.resolve("Bearer valid-token")).thenReturn("valid-token");
        when(jwtTokenProvider.getAccessTokenPayload("valid-token"))
                .thenReturn(new JwtTokenProvider.AccessTokenPayload(11L, "USER"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedMember.class);
        AuthenticatedMember authenticatedMember = (AuthenticatedMember) authentication.getPrincipal();
        assertThat(authenticatedMember.memberId()).isEqualTo(11L);
        assertThat(authenticatedMember.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("ServiceException 발생 시 인증 엔트리포인트를 호출한다")
    void doFilterInternal_whenServiceException() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, bearerTokenResolver, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        when(bearerTokenResolver.resolve("Bearer invalid-token"))
                .thenThrow(new ServiceException(
                        CommonErrorCode.UNAUTHORIZED,
                        "[JwtAuthenticationFilterTest#doFilterInternal_whenServiceException] invalid token",
                        "유효하지 않은 토큰입니다."));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint)
                .commence(eq(request), eq(response), any(InsufficientAuthenticationException.class));
        assertThat(request.getAttribute(RestAuthenticationEntryPoint.ERROR_MESSAGE_ATTRIBUTE))
                .isEqualTo("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("만료 토큰 예외는 원본 메시지를 유지한다")
    void doFilterInternal_whenExpiredTokenException() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, bearerTokenResolver, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token");
        when(bearerTokenResolver.resolve("Bearer expired-token"))
                .thenThrow(new TokenAuthenticationException(
                        TokenErrorType.EXPIRED,
                        "[JwtAuthenticationFilterTest#doFilterInternal_whenExpiredTokenException] token expired",
                        "만료된 토큰입니다."));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(authenticationEntryPoint)
                .commence(eq(request), eq(response), any(InsufficientAuthenticationException.class));
        assertThat(request.getAttribute(RestAuthenticationEntryPoint.ERROR_MESSAGE_ATTRIBUTE))
                .isEqualTo("만료된 토큰입니다.");
    }

    @Test
    @DisplayName("토큰 재발급 경로는 Authorization 헤더가 있어도 JWT 검증을 건너뛴다")
    void doFilterInternal_refreshPathShouldBypassJwtValidation() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, bearerTokenResolver, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/token/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(bearerTokenResolver, jwtTokenProvider, authenticationEntryPoint);
    }

    @Test
    @DisplayName("context-path가 있어도 토큰 재발급 경로는 JWT 검증을 건너뛴다")
    void doFilterInternal_refreshPathWithContextPathShouldBypassJwtValidation() throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, bearerTokenResolver, authenticationEntryPoint);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app/api/v1/auth/token/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.setContextPath("/app");
        request.setServletPath("/api/v1/auth/token/refresh");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(bearerTokenResolver, jwtTokenProvider, authenticationEntryPoint);
    }
}
