package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.auth.req.SignupRequest;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.dto.user.req.NicknameUpdateRequest;
import org.ktb.sideproject.dto.user.req.PasswordUpdateRequest;
import org.ktb.sideproject.dto.user.req.ProfileImageUpdateRequest;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.UserService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void signup(SignupRequest signupRequest) {

        if(!isEmailAvailable(signupRequest.email())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if(!isNicknameAvailable(signupRequest.nickname())){
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }


        User user = User.builder()
                .email(signupRequest.email())
                .password(passwordEncoder.encode(signupRequest.password()))
                .nickname(signupRequest.nickname())
                .profileImage("default-profile.png") // 나중에 이미지 추가시 변경
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return new UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImage());
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호 확인과 다릅니다.");
        }

        user.changePassword(passwordEncoder.encode(request.password()));
    }

    @Override
    @Transactional
    public UserInfo updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new IllegalArgumentException("중복된 닉네임 입니다.");
        }

        user.changeNickname(request.nickname());

        return new UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImage());
    }

    @Override
    @Transactional
    public UserInfo updateProfileImage(Long userId, ProfileImageUpdateRequest request) {
        User user = userRepository.findById(userId).
                orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        user.changeProfileImage(request.profileImage());
        return new  UserInfo(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImage());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        userRepository.delete(user);
    }
}
