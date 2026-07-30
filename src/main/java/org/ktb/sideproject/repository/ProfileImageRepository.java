package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.ProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {
    Optional<ProfileImage> findByImageUrlAndStatusAndUploaderId(String imageUrl, ImageStatus status, Long uploaderId);

    Optional<ProfileImage> findByUserIdAndStatus(Long userId, ImageStatus status);

    List<ProfileImage> findByUserIdInAndStatus(List<Long> userIds, ImageStatus status);

    List<ProfileImage> findByStatusAndCreatedAtBefore(ImageStatus status, LocalDateTime createdAt);
}
