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

    // 이미지 업로드
    @PostMapping("/posts/{postId}")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @AuthenticationPrincipal Long authUserId,
            @PathVariable Long postId,
            @RequestParam("image") MultipartFile multipartFile) {
        ImageUploadResponse imageUploadResponse = imageService.uploadImage(authUserId, postId, multipartFile);
        return ResponseEntity.ok(imageUploadResponse);
    }

    // 이미지 삭제
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @AuthenticationPrincipal Long authUserId,
            @PathVariable Long imageId) {
        imageService.deleteImage(authUserId, imageId);
        return ResponseEntity.noContent().build();
    }
}
