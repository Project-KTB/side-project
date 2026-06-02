package org.ktb.sideproject.dto.auth.req;


import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 주소 형식을 입력해주세요.")
        String email,
        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{8,20}$",
                message = "비밀번호는 8자 이상 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."
        )
        String password,
        @NotBlank(message = "비밀번호를 한번 더 입력해주세요.")
        String passwordConfirm,
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 10, message = "닉네임은 최대 10자까지 작성 가능합니다.")
        @Pattern(regexp = "^\\S+$", message = "띄어쓰기를 없애주세요.")
        String nickname,
        String profileImage) {

}
