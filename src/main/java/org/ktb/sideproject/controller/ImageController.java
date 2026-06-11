package org.ktb.sideproject.controller;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.ktb.sideproject.service.ImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    // 게시글 이미지 업로드
    @PostMapping("/posts")
    public ResponseEntity<ImageUploadResponse> uploadPostImage(
            @RequestParam("image") MultipartFile multipartFile) {
        ImageUploadResponse imageUploadResponse = imageService.uploadPostImage(multipartFile);
        return ResponseEntity.ok(imageUploadResponse);
    }

    // 프로필 이미지 업로드
    @PostMapping("/profile")
    public ResponseEntity<ImageUploadResponse> uploadProfileImage(
            @RequestParam("image") MultipartFile multipartFile) {
        ImageUploadResponse imageUploadResponse = imageService.uploadProfileImage(multipartFile);
        return ResponseEntity.ok(imageUploadResponse);
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
