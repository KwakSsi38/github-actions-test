package back.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import back.domain.auth.entity.RefreshToken;
import back.domain.auth.repository.RefreshTokenRepository;
import back.domain.member.entity.Member;
import back.domain.member.repository.MemberRepository;
import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import back.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

@Service
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring-managed singleton dependencies are intentionally injected by reference.")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final String INVALID_REFRESH_MESSAGE = "유효하지 않은 토큰입니다.";
    private static final String TOKEN_OWNER_MISMATCH_MESSAGE = "본인 토큰이 아닙니다.";
    private static final String MEMBER_NOT_FOUND_MESSAGE = "회원이 존재하지 않습니다.";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public AuthTokenResult refresh(String refreshToken) {
        long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        Member member = getMemberOrThrow(memberId);
        RefreshToken storedRefreshToken = getStoredRefreshTokenOrThrow(memberId);
        validateRefreshTokenMatch(storedRefreshToken, refreshToken);

        String accessToken =
                jwtTokenProvider.generateAccessToken(member.getId(), member.getEmail(), member.getRole().name());
        String rotatedRefreshToken =
                jwtTokenProvider.generateRefreshToken(member.getId(), member.getEmail(), member.getRole().name());
        storedRefreshToken.rotate(rotatedRefreshToken);
        refreshTokenRepository.save(storedRefreshToken);

        return new AuthTokenResult(accessToken, rotatedRefreshToken);
    }

    @Override
    @Transactional
    public void logout(long authenticatedMemberId, String refreshToken) {
        long tokenOwnerId = jwtTokenProvider.getMemberIdFromRefreshToken(refreshToken);
        if (authenticatedMemberId != tokenOwnerId) {
            throw forbiddenException(
                    "[AuthServiceImpl#logout] refresh token owner and authenticated member do not match");
        }

        RefreshToken storedRefreshToken = getStoredRefreshTokenOrThrow(tokenOwnerId);
        validateRefreshTokenMatch(storedRefreshToken, refreshToken);
        refreshTokenRepository.deleteByMemberId(authenticatedMemberId);
    }

    private Member getMemberOrThrow(long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new ServiceException(
                CommonErrorCode.NOT_FOUND,
                "[AuthServiceImpl#getMemberOrThrow] member not found by id",
                MEMBER_NOT_FOUND_MESSAGE));
    }

    private RefreshToken getStoredRefreshTokenOrThrow(long memberId) {
        return refreshTokenRepository.findByMemberId(memberId).orElseThrow(() -> unauthorizedException(
                "[AuthServiceImpl#getStoredRefreshTokenOrThrow] stored refresh token not found"));
    }

    private void validateRefreshTokenMatch(RefreshToken storedRefreshToken, String refreshToken) {
        if (!storedRefreshToken.matches(refreshToken)) {
            throw unauthorizedException("[AuthServiceImpl#validateRefreshTokenMatch] refresh token mismatch");
        }
    }

    private ServiceException unauthorizedException(String logMessage) {
        return new ServiceException(CommonErrorCode.UNAUTHORIZED, logMessage, INVALID_REFRESH_MESSAGE);
    }

    private ServiceException forbiddenException(String logMessage) {
        return new ServiceException(CommonErrorCode.FORBIDDEN, logMessage, TOKEN_OWNER_MISMATCH_MESSAGE);
    }
}
