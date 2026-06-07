package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.auth.JwtProvider;
import org.ktb.sideproject.dto.auth.LoginResult;
import org.ktb.sideproject.dto.auth.ReissueResult;
import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.entity.RefreshToken;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.RefreshTokenRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.AuthService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public LoginResult login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new UsernameNotFoundException("사용자가 존재하지 않습니다."));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtProvider.createAT(user.getId());
        // refreshToken 쿠키나 헤더 처리
        String refreshToken = jwtProvider.createRT(user.getId());

        refreshTokenRepository.save(new RefreshToken(user, refreshToken));


        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage()
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

        refreshTokenRepository.deleteByRefreshToken(refreshToken);
    }

    @Override
    @Transactional
    public ReissueResult reissueToken(String refreshToken) {
        if(refreshToken == null || !jwtProvider.validateRefreshToken(refreshToken)){
            throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("저장된 Refresh Token이 없습니다."));

        if(!savedToken.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("RefreshToken이 일치하지 않습니다.");
        }

        String newAccessToken = jwtProvider.createAT(userId);
        String newRefreshToken = jwtProvider.createRT(userId);

        savedToken.updateRefreshToken(newRefreshToken);

        return new ReissueResult(newAccessToken, newRefreshToken);
    }


}
