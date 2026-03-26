package back.domain.auth.service;

public interface AuthService {
    GoogleLoginResult loginWithGoogle(String idToken);

    AuthTokenResult refresh(String refreshToken);

    void logout(long authenticatedMemberId, String refreshToken);
}
