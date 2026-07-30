package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.like.res.PostLikeResponse;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.PostLike;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.PostLikeRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.PostLikeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PostLikeResponse likePost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new CustomException(ErrorCode.ALREADY_LIKED_POST);
        }

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        try {
            postLikeRepository.saveAndFlush(postLike);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.ALREADY_LIKED_POST);
        }

        postRepository.incrementLikesCount(postId);
        int likesCount = postRepository.findLikesCountById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return new PostLikeResponse(post.getId(), likesCount, true);
    }

    @Override
    @Transactional
    public PostLikeResponse unlikePost(Long userId, Long postId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        int deletedRows = postLikeRepository.deleteByUserIdAndPostId(userId, postId);
        if (deletedRows == 0) {
            throw new CustomException(ErrorCode.POST_LIKE_NOT_FOUND);
        }

        postRepository.decrementLikesCount(postId);
        int likesCount = postRepository.findLikesCountById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return new PostLikeResponse(post.getId(), likesCount, false);
    }
}
