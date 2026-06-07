package org.ktb.sideproject.dto.auth;

public record ReissueResult(
        String accessToken,
        String refreshToken
) {
}
