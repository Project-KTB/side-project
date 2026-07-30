package org.ktb.sideproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.post.req.PostCreateRequest;
import org.ktb.sideproject.dto.post.req.PostUpdateRequest;
import org.ktb.sideproject.dto.post.res.PostDetailResponse;
import org.ktb.sideproject.dto.post.res.PostListResponse;
import org.ktb.sideproject.dto.post.res.PostUpdateResponse;
import org.ktb.sideproject.service.PostService;
import org.ktb.sideproject.validation.PaginationValidator;
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
            @AuthenticationPrincipal Long authUserId,
            @Valid @RequestBody PostCreateRequest request) {
        PostDetailResponse postDetailResponse = postService.createPost(authUserId, request);
        return ResponseEntity.ok(postDetailResponse);
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<PostListResponse> getPostList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size) {
        int validatedSize = PaginationValidator.requireValidSize(size);
        PostListResponse postListResponse = postService.getPostList(keyword, cursor, validatedSize);
        return ResponseEntity.ok(postListResponse);
    }

    // 게시글 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long authUserId) {
        PostDetailResponse postDetailResponse = postService.getPost(postId, authUserId);
        return ResponseEntity.ok(postDetailResponse);
    }

    // 게시글 수정
    @PatchMapping("/{postId}")
    public ResponseEntity<PostUpdateResponse> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long authUserId,
            @Valid @RequestBody PostUpdateRequest request) {
        PostUpdateResponse postUpdateResponse = postService.updatePost(authUserId, postId, request);
        return ResponseEntity.ok(postUpdateResponse);
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long authUserId) {
        postService.deletePost(authUserId, postId);
        return ResponseEntity.noContent().build();
    }
}
