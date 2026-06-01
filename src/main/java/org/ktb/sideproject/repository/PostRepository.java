package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    Page<Post> findAllByOrderByIdDesc(Pageable pageable);
}
