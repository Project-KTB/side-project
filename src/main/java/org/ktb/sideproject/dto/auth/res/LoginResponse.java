package org.ktb.sideproject.dto.auth.res;

import org.ktb.sideproject.dto.user.UserInfo;

public record LoginResponse(
        String accessToken,
        UserInfo userinfo
) {
}
