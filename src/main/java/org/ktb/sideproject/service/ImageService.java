package org.ktb.sideproject.service;

import org.ktb.sideproject.dto.image.req.ImagePresignedUploadRequest;
import org.ktb.sideproject.dto.image.res.ImagePresignedUploadResponse;
import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {
    // 게시글 이미지 업로드
    ImageUploadResponse uploadPostImage(MultipartFile file);

    // 프로필 이미지 업로드
    ImageUploadResponse uploadProfileImage(MultipartFile file);

    // 게시글 이미지 presigned 업로드 URL 생성
    ImagePresignedUploadResponse createPostImagePresignedUrl(ImagePresignedUploadRequest request);

    // 프로필 이미지 presigned 업로드 URL 생성
    ImagePresignedUploadResponse createProfileImagePresignedUrl(ImagePresignedUploadRequest request);

    // 게시글 이미지 삭제
    void deletePostImage(Long userId, Long imageId);

}
