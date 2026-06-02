package org.ktb.sideproject.dto.comment.res;

import org.ktb.sideproject.entity.Comment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        Long authorId,
        String authorNickname
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUser().getId(),
                comment.getUser().getNickname()
        );
    }
}
