package org.ktb.sideproject.dto.auth.req;


public record SignupRequest(
        String email,
        String password,
        String nickname,
        String profileImage) {

}
