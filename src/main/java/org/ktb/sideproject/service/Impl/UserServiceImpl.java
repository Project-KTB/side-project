package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.auth.req.SignupRequest;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void signup(SignupRequest signupRequest) {

        if(userRepository.existsByEmail(signupRequest.email())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if(userRepository.existsByNickname(signupRequest.nickname())){
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = User.builder()
                .email(signupRequest.email())
                .password(passwordEncoder.encode(signupRequest.password()))
                .nickname(signupRequest.nickname())
                .profileImage("default-profile.png") // 나중에 이미지 S3 추가시 변경
                .build();

        userRepository.save(user);
    }

    @Override
    public void getUser() {

    }

    @Override
    public void updateUser() {

    }

    @Override
    public void deleteUser() {

    }
}
