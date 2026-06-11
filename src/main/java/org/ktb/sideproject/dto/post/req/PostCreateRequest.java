package org.ktb.sideproject.dto.post.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 26, message = "제목은 최대 26자까지 작성 가능합니다.")
        String title,
        @NotBlank(message = "내용을 입력해주세요.")
        String content,
        List<String> imageUrls
) {
}
