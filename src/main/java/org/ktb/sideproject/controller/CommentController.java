package org.ktb.sideproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.auth.CustomUserDetails;
import org.ktb.sideproject.dto.comment.req.CommentCreateRequest;
import org.ktb.sideproject.dto.comment.req.CommentUpdateRequest;
import org.ktb.sideproject.dto.comment.res.CommentResponse;
import org.ktb.sideproject.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/{postId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommentCreateRequest request) {
        Long userId = userDetails.getUserId();
        CommentResponse commentResponse = commentService.createComment(userId, postId, request);
        return ResponseEntity.ok(commentResponse);
    }

    // 댓글 수정
    @PatchMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CommentUpdateRequest request) {
        Long userId = userDetails.getUserId();
        CommentResponse commentResponse = commentService.updateComment(userId, commentId, request);
        return ResponseEntity.ok(commentResponse);
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        commentService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}
