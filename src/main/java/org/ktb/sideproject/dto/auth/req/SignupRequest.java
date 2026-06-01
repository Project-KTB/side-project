package org.ktb.sideproject.dto.auth.req;


import jakarta.validation.constraints.NotNull;

public record SignupRequest(
        @NotNull
        String email,
        @NotNull
        String password,
        @NotNull
        String nickname,
        String profileImage) {

}
