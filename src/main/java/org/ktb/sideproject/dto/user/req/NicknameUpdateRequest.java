package org.ktb.sideproject.dto.user.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 10, message = "닉네임은 최대 10자까지 작성 가능합니다.")
        @Pattern(regexp = "^\\S+$", message = "띄어쓰기를 없애주세요.")
        String nickname
) {
}
