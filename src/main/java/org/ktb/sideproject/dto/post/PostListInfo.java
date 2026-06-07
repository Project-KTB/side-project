package org.ktb.sideproject.dto.post;

import java.time.LocalDateTime;

public record PostListInfo(
        Long id,
        String title,
        int likesCount,
        int commentsCount,
        int viewsCount,
        LocalDateTime createdAt,
        Long authorId,
        String authorNickname,
        String authorProfileImage
) {
}
