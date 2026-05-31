package org.ktb.sideproject.service;


import org.ktb.sideproject.dto.auth.req.LoginRequest;
import org.ktb.sideproject.dto.auth.res.LoginResponse;

public interface AuthService {
    // 로그인
    public LoginResponse login(LoginRequest loginRequest);
    // 로그아웃
    public void logout();
}
