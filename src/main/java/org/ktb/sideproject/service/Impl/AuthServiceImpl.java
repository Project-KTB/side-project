package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.auth.JwtProvider;
import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.entity.User;
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

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new UsernameNotFoundException("사용자가 존재하지 않습니다."));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtProvider.createAT(user.getId());
        // refreshToken 쿠키나 헤더 처리
        // String refreshToken = jwtProvider.createRT(user.getId());

        UserInfo userInfo = new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage()
        );


        return new LoginResponse(accessToken, userInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public void logout() {

    }
}
