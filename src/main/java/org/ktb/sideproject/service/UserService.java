package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.auth.req.SignupRequest;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.dto.user.req.NicknameUpdateRequest;
import org.ktb.sideproject.dto.user.req.PasswordUpdateRequest;
import org.ktb.sideproject.dto.user.req.ProfileImageUpdateRequest;

public interface UserService {
    // 회원 가입
    void signup(SignupRequest signupRequest);
    // 회원 정보 조회
    UserInfo getUser(Long userId);
    // 회원 정보 수정
    void updatePassword(Long userId, PasswordUpdateRequest request);

    UserInfo updateNickname(Long userId, NicknameUpdateRequest request);

    UserInfo updateProfileImage(Long userId, ProfileImageUpdateRequest request);
    // 회원 탈퇴
    public void deleteUser(Long userId);
}
