package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.PostImageRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
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

    private final PostImageRepository postImageRepository;
    private final ProfileImageRepository profileImageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.image-url-prefix}")
    private String imageUrlPrefix;

    @Override
    public ImageUploadResponse uploadPostImage(MultipartFile file) {
        validateImage(file);

        String originName = file.getOriginalFilename();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        StoredImage storedImage = storeImage(file, "post-images", imageName);
        PostImage image = postImageRepository.save(new PostImage(
                originName,
                imageName,
                storedImage.imageUrl(),
                storedImage.storageKey()
        ));

        return new ImageUploadResponse(
                image.getId(),
                image.getOriginName(),
                image.getImageName(),
                image.getImageUrl(),
                image.getStorageKey()
        );
    }

    @Override
    @Transactional
    public ImageUploadResponse uploadProfileImage(MultipartFile file) {
        validateImage(file);

        String originName = file.getOriginalFilename();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        StoredImage storedImage = storeImage(file, "profile-images", imageName);
        ProfileImage image = profileImageRepository.save(new ProfileImage(
                originName,
                imageName,
                storedImage.imageUrl(),
                storedImage.storageKey()
        ));

        return new ImageUploadResponse(
                image.getId(),
                image.getOriginName(),
                image.getImageName(),
                image.getImageUrl(),
                image.getStorageKey()
        );
    }

    @Override
    @Transactional
    public void deletePostImage(Long userId, Long imageId) {
        PostImage image = postImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_NOT_FOUND));

        if (image.getPost() == null || !image.getPost().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.IMAGE_ACCESS_DENIED, "게시글 작성자만 이미지를 삭제할 수 있습니다.");
        }

        postImageRepository.delete(image);
        deleteImageFile(image.getStorageKey());
    }

    private void validateImage(MultipartFile file) {
        // 이미지 존재 유무
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.IMAGE_FILE_REQUIRED);
        }

        // 이미지 크기 제한
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }

        // Content-Type 확인
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }

        extractExtension(file.getOriginalFilename());
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new CustomException(ErrorCode.IMAGE_EXTENSION_REQUIRED);
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_EXTENSION);
        }

        return extension;
    }

    private StoredImage storeImage(MultipartFile file, String directory, String imageName) {
        String storageKey = directory + "/" + LocalDate.now().format(DATE_PATH_FORMATTER) + "/" + imageName;
        String imageUrl = normalizePrefix(imageUrlPrefix) + "/" + storageKey;
        Path savePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED, e);
        }

        return new StoredImage(storageKey, imageUrl);
    }

    private void deleteImageFile(String storageKey) {
        Path imagePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_DELETE_FAILED, e);
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

    private record StoredImage(
            String storageKey,
            String imageUrl
    ) {
    }
}
