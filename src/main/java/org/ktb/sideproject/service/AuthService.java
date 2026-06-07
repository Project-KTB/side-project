package org.ktb.sideproject.service;


import org.ktb.sideproject.dto.auth.LoginResult;
import org.ktb.sideproject.dto.auth.ReissueResult;
import org.ktb.sideproject.dto.auth.req.LoginRequest;

public interface AuthService {
    // 로그인
    LoginResult login(LoginRequest loginRequest);
    // 로그아웃
    void logout(String refreshToken, Long userId);
    // AT 재발급
    ReissueResult reissueToken(String refreshToken);
}
