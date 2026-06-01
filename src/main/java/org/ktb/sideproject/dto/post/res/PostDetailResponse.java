package org.ktb.sideproject.dto.post.res;

import org.ktb.sideproject.entity.Post;

import java.time.LocalDateTime;

//게시글 상세조회
public record PostDetailResponse(
        Long id,
        String title,
        String content,
        String imageUrl,
        int likesCount,
        int commentsCount,
        int viewsCount,
        LocalDateTime createdAt,
        Long authorId
) {
    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl(),
                post.getLikesCount(),
                post.getCommentsCount(),
                post.getViewsCount(),
                post.getCreatedAt(),
                post.getUser().getId()
        );
    }
}
