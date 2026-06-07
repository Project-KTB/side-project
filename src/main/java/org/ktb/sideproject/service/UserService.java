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
    // 회원 정보 수정(비밀번호)
    void updatePassword(Long userId, PasswordUpdateRequest request);
    // 회원 정보 수정(닉네임)
    UserInfo updateNickname(Long userId, NicknameUpdateRequest request);
    // 회원 정보 수정(프로필 이미지)
    UserInfo updateProfileImage(Long userId, ProfileImageUpdateRequest request);
    // 회원 탈퇴
    public void deleteUser(Long userId);

    // 이메일 중복 체크
    boolean isEmailAvailable(String email);

    // 닉네임 중복 체크
    boolean isNicknameAvailable(String nickname);

}
