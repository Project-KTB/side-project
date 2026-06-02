package org.ktb.sideproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.auth.CustomUserDetails;
import org.ktb.sideproject.dto.post.req.PostCreateRequest;
import org.ktb.sideproject.dto.post.req.PostUpdateRequest;
import org.ktb.sideproject.dto.post.res.PostDetailResponse;
import org.ktb.sideproject.dto.post.res.PostListResponse;
import org.ktb.sideproject.dto.post.res.PostUpdateResponse;
import org.ktb.sideproject.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    // 게시글 생성
    @PostMapping
    public ResponseEntity<PostDetailResponse> createPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostCreateRequest request) {
        Long userId = userDetails.getUserId();
        PostDetailResponse postDetailResponse = postService.createPost(userId, request);
        return ResponseEntity.ok(postDetailResponse);
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<PostListResponse> getPostList(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        if(size <= 0) {
            throw new IllegalArgumentException("size는 0보다 커야합니다.");
        }
        PostListResponse postListResponse = postService.getPostList(cursor, size);
        return ResponseEntity.ok(postListResponse);
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long postId) {
        PostDetailResponse  postDetailResponse = postService.getPost(postId);
        return ResponseEntity.ok(postDetailResponse);
    }

    // 게시글 수정
    @PatchMapping("/{postId}")
    public ResponseEntity<PostUpdateResponse> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostUpdateRequest request) {
        Long userId = userDetails.getUserId();
        PostUpdateResponse postUpdateResponse = postService.updatePost(userId, postId, request);
        return ResponseEntity.ok(postUpdateResponse);
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }
}
