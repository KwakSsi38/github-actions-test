package back.domain.auth.service;

public interface AuthService {
    AuthTokenResult refresh(String refreshToken);

    void logout(long authenticatedMemberId, String refreshToken);
}
