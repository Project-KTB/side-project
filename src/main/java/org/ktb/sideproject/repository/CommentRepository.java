package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = "user")
    Page<Comment> findByPostIdOrderByIdDesc(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Comment> findByPostIdAndIdLessThanOrderByIdDesc(Long postId, Long cursor, Pageable pageable);

    @EntityGraph(attributePaths = "post")
    List<Comment> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "post"})
    @Query("""
            SELECT c
            FROM Comment c
            WHERE c.id = :commentId
            """)
    Optional<Comment> findWithUserAndPostById(@Param("commentId") Long commentId);

    void deleteByPostIdIn(List<Long> postIds);
}
