package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.auth.req.SignupRequest;

public interface UserService {
    // 회원 가입
    public void signup(SignupRequest signupRequest);
    // 회원 정보 조회
    public void getUser();
    // 회원 정보 수정
    public void updateUser();
    // 회원 탈퇴
    public void deleteUser();
}
