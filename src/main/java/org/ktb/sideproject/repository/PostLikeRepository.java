package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.PostLike;
import org.ktb.sideproject.entity.PostLikeId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @EntityGraph(attributePaths = "post")
    List<PostLike> findByUserId(Long userId);

    void deleteByPostIdIn(List<Long> postIds);

    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM PostLike pl
            WHERE pl.user.id = :userId
              AND pl.post.id = :postId
            """)
    int deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

}
