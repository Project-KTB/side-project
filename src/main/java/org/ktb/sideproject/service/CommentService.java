package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.comment.req.CommentCreateRequest;
import org.ktb.sideproject.dto.comment.req.CommentUpdateRequest;
import org.ktb.sideproject.dto.comment.res.CommentResponse;

public interface CommentService {
    // 댓글 작성
    CommentResponse createComment(Long userId, Long postId, CommentCreateRequest request);

    // 댓글 수정
    CommentResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request);

    // 댓글 삭제
    void deleteComment(Long userId, Long commentId);
}
