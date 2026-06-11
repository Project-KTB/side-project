package org.ktb.sideproject.dto.post.req;

import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @Size(max = 26, message = "제목은 최대 26자까지 작성 가능합니다.")
        String title,
        String content,
        List<String> imageUrls
) {
    public boolean hasUpdateValue() {
        return hasText(title) || hasText(content) || imageUrls != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
