package org.ktb.sideproject.controller;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.image.req.ImagePresignedUploadRequest;
import org.ktb.sideproject.dto.image.res.ImagePresignedUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.ktb.sideproject.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Value("${app.image-upload.enabled:true}")
    private boolean imageUploadEnabled;

    // 게시글 이미지 업로드
    @PostMapping("/posts")
    public ResponseEntity<ImageUploadResponse> uploadPostImage(
            @RequestParam("image") MultipartFile multipartFile) {
        assertImageUploadEnabled();
        ImageUploadResponse imageUploadResponse = imageService.uploadPostImage(multipartFile);
        return ResponseEntity.ok(imageUploadResponse);
    }

    // 프로필 이미지 업로드
    @PostMapping("/profile")
    public ResponseEntity<ImageUploadResponse> uploadProfileImage(
            @RequestParam("image") MultipartFile multipartFile) {
        assertImageUploadEnabled();
        ImageUploadResponse imageUploadResponse = imageService.uploadProfileImage(multipartFile);
        return ResponseEntity.ok(imageUploadResponse);
    }

    // 게시글 이미지 presigned URL 발급
    @PostMapping("/posts/presigned-url")
    public ResponseEntity<ImagePresignedUploadResponse> createPostImagePresignedUrl(
            @RequestBody ImagePresignedUploadRequest request) {
        assertImageUploadEnabled();
        ImagePresignedUploadResponse response = imageService.createPostImagePresignedUrl(request);
        return ResponseEntity.ok(response);
    }

    // 프로필 이미지 presigned URL 발급
    @PostMapping("/profile/presigned-url")
    public ResponseEntity<ImagePresignedUploadResponse> createProfileImagePresignedUrl(
            @RequestBody ImagePresignedUploadRequest request) {
        assertImageUploadEnabled();
        ImagePresignedUploadResponse response = imageService.createProfileImagePresignedUrl(request);
        return ResponseEntity.ok(response);
    }

    private void assertImageUploadEnabled() {
        if (!imageUploadEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Image upload is disabled for this deployment.");
        }
    }

    // 게시글 이미지 삭제
    @DeleteMapping("/posts/{imageId}")
    public ResponseEntity<Void> deletePostImage(
            @AuthenticationPrincipal Long authUserId,
            @PathVariable Long imageId) {
        imageService.deletePostImage(authUserId, imageId);
        return ResponseEntity.noContent().build();
    }
}
