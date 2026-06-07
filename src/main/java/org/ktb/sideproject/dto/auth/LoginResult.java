package org.ktb.sideproject.dto.auth;

import org.ktb.sideproject.dto.auth.res.LoginResponse;

public record LoginResult(
        LoginResponse loginResponse,
        String refreshToken
) {
}
