package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.like.res.PostLikeResponse;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.PostLike;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.PostLikeRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.PostLikeService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new IllegalArgumentException("이미 좋아요를 누른 게시글입니다.");
        }

        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        postLikeRepository.save(postLike);
        post.increaseLikesCount();

        return new PostLikeResponse(post.getId(), post.getLikesCount(), true);
    }

    @Override
    @Transactional
    public PostLikeResponse unlikePost(Long userId, Long postId) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        PostLike postLike = postLikeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new IllegalArgumentException("좋아요를 누르지 않은 게시글입니다."));

        postLikeRepository.delete(postLike);
        post.decreaseLikesCount();

        return new PostLikeResponse(post.getId(), post.getLikesCount(), false);
    }
}
