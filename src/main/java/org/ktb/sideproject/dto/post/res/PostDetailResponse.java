package org.ktb.sideproject.dto.post.res;

import org.ktb.sideproject.entity.Post;

import java.time.LocalDateTime;

//게시글 상세조회
public record PostDetailResponse(
        Long id,
        String title,
        String content,
        int likesCount,
        int commentsCount,
        int viewsCount,
        LocalDateTime createdAt,
        Long authorId
        // 이미지 리스트 URL
) {
    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getLikesCount(),
                post.getCommentsCount(),
                post.getViewsCount(),
                post.getCreatedAt(),
                post.getUser().getId()
        );
    }
}
