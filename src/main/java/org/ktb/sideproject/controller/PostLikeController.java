package org.ktb.sideproject.controller;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.like.res.PostLikeResponse;
import org.ktb.sideproject.service.PostLikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    // 게시글 좋아요
    @PostMapping("/{postId}/likes")
    public ResponseEntity<PostLikeResponse> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long authUserId) {

        PostLikeResponse postLikeResponse = postLikeService.likePost(authUserId, postId);
        return ResponseEntity.ok(postLikeResponse);
    }

    // 게시글 좋아요 취소
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<PostLikeResponse> unlikePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long authUserId) {
        PostLikeResponse postLikeResponse = postLikeService.unlikePost(authUserId, postId);
        return ResponseEntity.ok(postLikeResponse);
    }
}
