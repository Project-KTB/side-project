package org.ktb.sideproject.dto.auth.req;

import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull
        String email,
        @NotNull
        String password
) {
}
