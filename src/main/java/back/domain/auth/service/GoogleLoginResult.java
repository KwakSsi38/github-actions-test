package back.domain.auth.service;

public record GoogleLoginResult(
        long memberId,
        String name,
        String email,
        String role,
        String accessToken,
        String refreshToken) {}
