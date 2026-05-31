package org.ktb.sideproject.dto.auth.req;

public record LoginRequest(
        String email,
        String password
) {
}
