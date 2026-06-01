package org.ktb.sideproject.dto.post.req;

public record PostUpdateRequest(
        String title,
        String content,
        String imageUrl
) {
    public boolean hasUpdateValue() {
        return hasText(title) || hasText(content) || hasText(imageUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
