package org.ktb.sideproject.dto.user.res;

public record UserInfo(
        Long id,
        String email,
        String nickname,
        String profileImage
) {
}