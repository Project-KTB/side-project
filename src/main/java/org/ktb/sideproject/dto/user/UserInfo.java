package org.ktb.sideproject.dto.user;

public record UserInfo(
        Long id,
        String email,
        String nickname,
        String profileImage
) {
}