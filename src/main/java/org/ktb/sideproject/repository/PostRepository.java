package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT p
            FROM Post p
            WHERE (:cursor IS NULL OR p.id < :cursor)
              AND (
                :keyword IS NULL
                OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY p.id DESC
            """)
    Page<Post> searchByKeywordOrderByIdDesc(
            @Param("keyword") String keyword,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT p
            FROM Post p
            LEFT JOIN FETCH p.images
            WHERE p.user.id = :userId
            """)
    List<Post> findByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT p
            FROM Post p
            JOIN FETCH p.user
            LEFT JOIN FETCH p.images
            WHERE p.id = :postId
            """)
    Optional<Post> findDetailById(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.viewsCount = p.viewsCount + 1
            WHERE p.id = :postId
            """)
    int incrementViewsCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.likesCount = p.likesCount + 1
            WHERE p.id = :postId
            """)
    int incrementLikesCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.likesCount = CASE WHEN p.likesCount > 0 THEN p.likesCount - 1 ELSE 0 END
            WHERE p.id = :postId
            """)
    int decrementLikesCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.commentsCount = p.commentsCount + 1
            WHERE p.id = :postId
            """)
    int incrementCommentsCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Post p
            SET p.commentsCount = CASE WHEN p.commentsCount > 0 THEN p.commentsCount - 1 ELSE 0 END
            WHERE p.id = :postId
            """)
    int decrementCommentsCount(@Param("postId") Long postId);

    @Query("""
            SELECT p.likesCount
            FROM Post p
            WHERE p.id = :postId
            """)
    Optional<Integer> findLikesCountById(@Param("postId") Long postId);
}
