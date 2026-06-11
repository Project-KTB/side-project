package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    Page<Post> findAllByOrderByIdDesc(Pageable pageable);

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

    List<Post> findByUserId(Long userId);
}
