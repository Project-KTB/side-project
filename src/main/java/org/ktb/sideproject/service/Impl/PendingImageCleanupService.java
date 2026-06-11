package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.repository.PostImageRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingImageCleanupService {

    private final PostImageRepository postImageRepository;
    private final ProfileImageRepository profileImageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredPendingImages() {
        LocalDateTime expiredAt = LocalDateTime.now().minusHours(24);
        List<PostImage> postImages = postImageRepository.findByStatusAndCreatedAtBefore(ImageStatus.PENDING, expiredAt);
        List<ProfileImage> profileImages = profileImageRepository.findByStatusAndCreatedAtBefore(ImageStatus.PENDING, expiredAt);
        List<PostImage> deletedPostImages = new ArrayList<>();
        List<ProfileImage> deletedProfileImages = new ArrayList<>();

        for (PostImage image : postImages) {
            if (deleteImageFile(image.getStorageKey())) {
                deletedPostImages.add(image);
            }
        }

        for (ProfileImage image : profileImages) {
            if (deleteImageFile(image.getStorageKey())) {
                deletedProfileImages.add(image);
            }
        }

        postImageRepository.deleteAll(deletedPostImages);
        profileImageRepository.deleteAll(deletedProfileImages);
    }

    private boolean deleteImageFile(String storageKey) {
        Path imagePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.deleteIfExists(imagePath);
            return true;
        } catch (IOException e) {
            log.warn("만료된 PENDING 이미지 파일 삭제에 실패했습니다. storageKey={}", storageKey, e);
            return false;
        }
    }
}
