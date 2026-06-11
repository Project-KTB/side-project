package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostIdOrderByIdDesc(Long postId, Pageable pageable);

    Page<Comment> findByPostIdAndIdLessThanOrderByIdDesc(Long postId, Long cursor, Pageable pageable);

    List<Comment> findByUserId(Long userId);

    void deleteByPostIdIn(List<Long> postIds);
}
