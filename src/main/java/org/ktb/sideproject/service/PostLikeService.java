package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.like.res.PostLikeResponse;

public interface PostLikeService {
    // 게시글 좋아요
    PostLikeResponse likePost(Long userId, Long postId);

    // 게시글 좋아요 취소
    PostLikeResponse unlikePost(Long userId, Long postId);
}
