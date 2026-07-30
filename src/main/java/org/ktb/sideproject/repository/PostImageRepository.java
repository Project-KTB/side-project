package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    List<PostImage> findByImageUrlInAndStatusAndUploaderId(List<String> imageUrls, ImageStatus status, Long uploaderId);

    List<PostImage> findByStatusAndCreatedAtBefore(ImageStatus status, LocalDateTime createdAt);
}
