package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ktb.sideproject.auth.JwtProvider;
import org.ktb.sideproject.dto.auth.LoginResult;
import org.ktb.sideproject.dto.auth.ReissueResult;
import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.entity.RefreshToken;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.repository.RefreshTokenRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ProfileImageRepository profileImageRepository;
    private final RefreshTokenHasher refreshTokenHasher;

    @Override
    @Transactional
    public LoginResult login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.createAT(user.getId());
        // refreshToken 쿠키나 헤더 처리
        String refreshToken = jwtProvider.createRT(user.getId());

        refreshTokenRepository.save(new RefreshToken(user, refreshTokenHasher.hash(refreshToken)));

        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                getProfileImageUrl(user.getId())
        );
        LoginResponse loginResponse = new LoginResponse(accessToken, userInfo);

        return new LoginResult(loginResponse, refreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, Long userId) {
        if (refreshToken == null || userId == null) {
            return;
        }

        refreshTokenRepository.deleteByRefreshTokenHash(refreshTokenHasher.hash(refreshToken));
    }

    @Override
    @Transactional
    public ReissueResult reissueToken(String refreshToken) {
        if (refreshToken == null || !jwtProvider.validateRefreshToken(refreshToken)) {
            log.warn("[TOKEN_REISSUE_FAIL] Refresh Token 누락 또는 검증 실패");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        log.info("[TOKEN_REISSUE] Refresh Token 검증 성공. userId={}", userId);

        String refreshTokenHash = refreshTokenHasher.hash(refreshToken);
        RefreshToken savedToken = refreshTokenRepository.findByRefreshTokenHash(refreshTokenHash)
                .orElseThrow(() -> {
                    log.warn("[TOKEN_REISSUE_FAIL] 저장된 Refresh Token 없음. userId={}", userId);
                    return new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

        if (!savedToken.getUser().getId().equals(userId)) {
            log.warn("[TOKEN_REISSUE_FAIL] Refresh Token 사용자 불일치. tokenUserId={}, savedUserId={}",
                    userId, savedToken.getUser().getId());
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtProvider.createAT(userId);
        String newRefreshToken = jwtProvider.createRT(userId);

        savedToken.updateRefreshTokenHash(refreshTokenHasher.hash(newRefreshToken));
        log.info("[TOKEN_REISSUE] Access Token 및 Refresh Token 재발급 완료. userId={}", userId);

        return new ReissueResult(newAccessToken, newRefreshToken);
    }

    private String getProfileImageUrl(Long userId) {
        return profileImageRepository.findByUserIdAndStatus(userId, ImageStatus.SAVED)
                .map(ProfileImage::getImageUrl)
                .orElse("default-profile.png");
    }

}
