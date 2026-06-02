package org.ktb.sideproject.dto.auth.res;

import org.ktb.sideproject.dto.user.res.UserInfo;

public record LoginResponse(
        String accessToken,
        UserInfo userinfo
) {
}
