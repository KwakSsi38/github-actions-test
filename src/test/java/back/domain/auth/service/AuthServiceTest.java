package back.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import back.domain.auth.entity.RefreshToken;
import back.domain.auth.repository.RefreshTokenRepository;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(jwtTokenProvider, memberRepository, refreshTokenRepository);
    }

    @Test
    @DisplayName("유효한 refresh 토큰이면 access/refresh를 재발급하고 refresh를 회전한다")
    void refresh_success() {
        Member member = createMember(1L, "user@example.com");
        when(jwtTokenProvider.getMemberIdFromRefreshToken("old-refresh")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.generateAccessToken(1L, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(1L, "user@example.com", "USER")).thenReturn("new-refresh");
        when(refreshTokenRepository.rotateIfMatch(1L, "old-refresh", "new-refresh")).thenReturn(1);

        AuthTokenResult result = authService.refresh("old-refresh");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenRepository).rotateIfMatch(1L, "old-refresh", "new-refresh");
    }

    @Test
    @DisplayName("조건부 회전이 실패하면 401 예외를 던진다")
    void refresh_whenConditionalRotateFailed() {
        Member member = createMember(1L, "user@example.com");
        when(jwtTokenProvider.getMemberIdFromRefreshToken("old-refresh")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.generateAccessToken(1L, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(1L, "user@example.com", "USER")).thenReturn("new-refresh");
        when(refreshTokenRepository.rotateIfMatch(1L, "old-refresh", "new-refresh")).thenReturn(0);

        assertThatThrownBy(() -> authService.refresh("old-refresh"))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("refresh 토큰의 사용자 정보가 없으면 404 예외를 던진다")
    void refresh_whenMemberNotFound() {
        when(jwtTokenProvider.getMemberIdFromRefreshToken("old-refresh")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-refresh"))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
                });
    }

    @Test
    @DisplayName("로그아웃은 본인 refresh 토큰이면 저장소에서 제거한다")
    void logout_success() {
        when(jwtTokenProvider.getMemberIdFromRefreshToken("refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findByMemberId(1L))
                .thenReturn(Optional.of(RefreshToken.issue(1L, "refresh-token")));

        authService.logout(1L, "refresh-token");

        verify(refreshTokenRepository).deleteByMemberId(1L);
    }

    @Test
    @DisplayName("로그아웃 요청 토큰이 본인 것이 아니면 403 예외를 던진다")
    void logout_whenTokenOwnerMismatch() {
        when(jwtTokenProvider.getMemberIdFromRefreshToken("refresh-token")).thenReturn(2L);

        assertThatThrownBy(() -> authService.logout(1L, "refresh-token"))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("로그아웃 시 저장된 refresh 토큰이 없으면 401 예외를 던진다")
    void logout_whenStoredTokenMissing() {
        when(jwtTokenProvider.getMemberIdFromRefreshToken("refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(1L, "refresh-token"))
                .isInstanceOf(ServiceException.class)
                .satisfies(exception -> {
                    ServiceException serviceException = (ServiceException) exception;
                    assertThat(serviceException.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED);
                });
    }

    private Member createMember(long memberId, String email) {
        Member member = Member.createUser("google-sub-%d".formatted(memberId), email, "User %d".formatted(memberId));
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
