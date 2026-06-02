package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
}
