package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.post.req.PostCreateRequest;
import org.ktb.sideproject.dto.post.req.PostUpdateRequest;
import org.ktb.sideproject.dto.post.res.PostListResponse;
import org.ktb.sideproject.dto.post.res.PostDetailResponse;
import org.ktb.sideproject.dto.post.res.PostUpdateResponse;

public interface PostService {
    // 게시글 생성
    PostDetailResponse createPost(Long userId, PostCreateRequest request);
    // 게시글 목록 조회
    PostListResponse getPostList(String keyword, Long cursor, int size);
    // 게시글 상세 조회
    PostDetailResponse getPost(Long postId, Long userId);
    // 게시글 수정
    PostUpdateResponse updatePost(Long userId, Long postId, PostUpdateRequest request);
    // 게시글 삭제
    void deletePost(Long userId, Long postId);
}
