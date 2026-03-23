package back.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import back.global.exception.ServiceException;
import back.global.security.TokenAuthenticationException.TokenErrorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String INVALID_TOKEN_MESSAGE = "유효하지 않은 토큰입니다.";

    private final JwtTokenProvider jwtTokenProvider;
    private final BearerTokenResolver bearerTokenResolver;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = bearerTokenResolver.resolve(authorizationHeader);
            JwtTokenProvider.AccessTokenPayload payload = jwtTokenProvider.getAccessTokenPayload(accessToken);

            AuthenticatedMember authenticatedMember = new AuthenticatedMember(payload.memberId(), payload.role());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedMember, null, List.of(new SimpleGrantedAuthority("ROLE_" + payload.role())));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ServiceException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute(
                    RestAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE,
                    exception.getRsData().resultCode());
            request.setAttribute(
                    RestAuthenticationEntryPoint.ERROR_MESSAGE_ATTRIBUTE, normalizeErrorMessage(exception));
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException(
                            exception.getRsData().msg(), exception));
        }
    }

    private String normalizeErrorMessage(ServiceException exception) {
        if (exception instanceof TokenAuthenticationException tokenException) {
            if (tokenException.tokenErrorType() == TokenErrorType.EXPIRED) {
                return tokenException.getRsData().msg();
            }
        }

        return INVALID_TOKEN_MESSAGE;
    }
}
