package org.ktb.sideproject.auth;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new CustomUserDetails(userRepository.findByEmail(username)
                .orElseThrow(() ->new UsernameNotFoundException("사용자가 존재하지 않는다.")));
    }

    public UserDetails loadUserById(Long userId) {
        return new CustomUserDetails(userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자가 존재하지 않습니다.")));
    }

}
