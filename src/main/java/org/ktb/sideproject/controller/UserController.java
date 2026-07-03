package org.ktb.sideproject.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.auth.req.SignupRequest;
import org.ktb.sideproject.dto.user.req.NicknameUpdateRequest;
import org.ktb.sideproject.dto.user.req.PasswordUpdateRequest;
import org.ktb.sideproject.dto.user.req.ProfileImageUpdateRequest;
import org.ktb.sideproject.dto.user.res.DuplicateCheckResponse;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원 가입
    @PostMapping
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest signupRequest) {
        userService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료 되었습니다.");
    }

    // 이메일 중복 체크
    @GetMapping("/email/check")
    public ResponseEntity<DuplicateCheckResponse> checkEmail(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);

        if (!available) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DuplicateCheckResponse(false));
        }

        return ResponseEntity.ok(new DuplicateCheckResponse(true));
    }

    // 닉네임 중복 체크
    @GetMapping("/nickname/check")
    public ResponseEntity<DuplicateCheckResponse> checkNickname(@RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);

        if (!available) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new DuplicateCheckResponse(false));
        }

        return ResponseEntity.ok(new DuplicateCheckResponse(true));
    }

    // 회원정보 조회
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfo> getUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long authUserId) {
        if(!authUserId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인 정보만 조회할 수 있습니다.");
        }

        UserInfo userInfo = userService.getUser(userId);
        return ResponseEntity.ok(userInfo);
    }

    // 회원정보 수정(비밀번호)
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody PasswordUpdateRequest passwordUpdateRequest,
            @AuthenticationPrincipal Long authUserId) {
        if(!authUserId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인 정보만 수정할 수 있습니다.");
        }
        userService.updatePassword(userId, passwordUpdateRequest);
        return ResponseEntity.noContent().build();
    }

    // 회원정보 수정(닉네임)
    @PatchMapping("/{userId}/nickname")
    public ResponseEntity<UserInfo> updateNickname(
            @PathVariable Long userId,
            @Valid @RequestBody NicknameUpdateRequest nicknameUpdateRequest,
            @AuthenticationPrincipal Long authUserId) {
        if(!authUserId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인 정보만 수정할 수 있습니다.");
        }
        UserInfo userInfo = userService.updateNickname(userId, nicknameUpdateRequest);
        return ResponseEntity.ok(userInfo);
    }
    // 회원정보 수정(프로필 이미지)
    @PatchMapping("/{userId}/profileImage")
    public ResponseEntity<UserInfo> updateProfileImage(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileImageUpdateRequest profileImageUpdateRequest,
            @AuthenticationPrincipal Long authUserId) {
        if(!authUserId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인 정보만 수정할 수 있습니다.");
        }
        UserInfo userInfo = userService.updateProfileImage(userId, profileImageUpdateRequest);
        return ResponseEntity.ok(userInfo);
    }
    // 회원 탈퇴
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long authUserId) {
        if(!authUserId.equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인 정보만 삭제할 수 있습니다.");
        }
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }



}
