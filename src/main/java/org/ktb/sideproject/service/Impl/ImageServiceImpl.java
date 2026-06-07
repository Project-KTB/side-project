package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.ktb.sideproject.entity.Image;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.repository.ImageRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final PostRepository postRepository;
    private final ImageRepository imageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.image-url-prefix}")
    private String imageUrlPrefix;

    @Override
    @Transactional
    public ImageUploadResponse uploadImage(Long userId, Long postId, MultipartFile file) {
        validateImage(file);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자만 이미지를 업로드할 수 있습니다.");
        }

        String originName = file.getOriginalFilename();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        String storageKey = "images/" + LocalDate.now().format(DATE_PATH_FORMATTER) + "/" + imageName;
        String imageUrl = normalizePrefix(imageUrlPrefix) + "/" + storageKey;
        Path savePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }

        Image image = imageRepository.save(new Image(originName, imageName, imageUrl, post));

        return new ImageUploadResponse(
                image.getId(),
                image.getOriginName(),
                image.getImageName(),
                image.getImageUrl()
        );
    }

    @Override
    @Transactional
    public void deleteImage(Long userId, Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        if (!image.getPost().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자만 이미지를 삭제할 수 있습니다.");
        }

        imageRepository.delete(image);
        deleteImageFile(image.getImageUrl());
    }

    private void validateImage(MultipartFile file) {
        // 이미지 존재 유무
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 첨부해주세요.");
        }

        // 이미지 크기 제한
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
        }

        // 파일 확장자 확인
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("jpg, png, gif, webp 이미지만 업로드할 수 있습니다.");
        }

        extractExtension(file.getOriginalFilename());
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new IllegalArgumentException("이미지 파일 확장자가 필요합니다.");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 확장자입니다.");
        }

        return extension;
    }

    private void deleteImageFile(String imageUrl) {
        String prefix = normalizePrefix(imageUrlPrefix);
        String storageKey = imageUrl.startsWith(prefix + "/")
                ? imageUrl.substring(prefix.length() + 1)
                : imageUrl;
        Path imagePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 파일 삭제에 실패했습니다.", e);
        }
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        return prefix.endsWith("/")
                ? prefix.substring(0, prefix.length() - 1)
                : prefix;
    }
}
