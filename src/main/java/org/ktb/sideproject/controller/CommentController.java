package org.ktb.sideproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.comment.req.CommentCreateRequest;
import org.ktb.sideproject.dto.comment.req.CommentUpdateRequest;
import org.ktb.sideproject.dto.comment.res.CommentListResponse;
import org.ktb.sideproject.dto.comment.res.CommentResponse;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 조회
    @GetMapping("/{postId}")
    public ResponseEntity<CommentListResponse> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        if(size <= 0) {
            throw new CustomException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }

        CommentListResponse commentListResponse = commentService.getComments(postId, cursor, size);

        return ResponseEntity.ok(commentListResponse);
    }
    // 댓글 작성
    @PostMapping("/{postId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long authUserId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse commentResponse = commentService.createComment(authUserId, postId, request);
        return ResponseEntity.ok(commentResponse);
    }

    // 댓글 수정
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long authUserId,
            @Valid @RequestBody CommentUpdateRequest request) {
        CommentResponse commentResponse = commentService.updateComment(authUserId, commentId, request);
        return ResponseEntity.ok(commentResponse);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long authUserId) {
        commentService.deleteComment(authUserId, commentId);
        return ResponseEntity.noContent().build();
    }
}
