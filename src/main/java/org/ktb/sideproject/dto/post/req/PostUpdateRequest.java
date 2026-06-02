package org.ktb.sideproject.dto.post.req;

public record PostUpdateRequest(
        String title,
        String content
) {
    public boolean hasUpdateValue() {
        return hasText(title) || hasText(content);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
