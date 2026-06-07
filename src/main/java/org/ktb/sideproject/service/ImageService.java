package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    // 이미지 업로드
    ImageUploadResponse uploadImage(Long userId, Long postId, MultipartFile file);

    // 이미지 삭제
    void deleteImage(Long userId, Long imageId);

}
