package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.image.req.ImagePresignedUploadRequest;
import org.ktb.sideproject.dto.image.res.ImagePresignedUploadResponse;
import org.ktb.sideproject.dto.image.res.ImageUploadResponse;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.PostImageRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.service.ImageService;
import org.ktb.sideproject.service.storage.DeferredImageDeletionService;
import org.ktb.sideproject.service.storage.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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
    private static final int MAGIC_BYTES_READ_LIMIT = 12;

    private final PostImageRepository postImageRepository;
    private final ProfileImageRepository profileImageRepository;
    private final ImageStorageService imageStorageService;
    private final DeferredImageDeletionService deferredImageDeletionService;

    @Override
    @Transactional
    public ImageUploadResponse uploadPostImage(Long uploaderId, MultipartFile file) {
        requireUploader(uploaderId);
        validateImage(file);

        String originName = file.getOriginalFilename();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        String storageKey = createStorageKey("post-images", imageName);
        ImageStorageService.StoredImage storedImage = imageStorageService.store(file, storageKey);
        PostImage image = postImageRepository.save(new PostImage(
                originName,
                imageName,
                storedImage.imageUrl(),
                storedImage.storageKey(),
                uploaderId
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
    public ImageUploadResponse uploadProfileImage(Long uploaderId, MultipartFile file) {
        requireUploader(uploaderId);
        validateImage(file);

        String originName = file.getOriginalFilename();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        String storageKey = createStorageKey("profile-images", imageName);
        ImageStorageService.StoredImage storedImage = imageStorageService.store(file, storageKey);
        ProfileImage image = profileImageRepository.save(new ProfileImage(
                originName,
                imageName,
                storedImage.imageUrl(),
                storedImage.storageKey(),
                uploaderId
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
    public ImagePresignedUploadResponse createPostImagePresignedUrl(Long uploaderId, ImagePresignedUploadRequest request) {
        requireUploader(uploaderId);
        validateImageMetadata(request);

        String originName = request.originName();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        String storageKey = createStorageKey("post-images", imageName);
        ImageStorageService.PresignedUpload presignedUpload = imageStorageService.presignPut(storageKey, request.contentType(), request.fileSize());
        PostImage image = postImageRepository.save(new PostImage(
                originName,
                imageName,
                presignedUpload.imageUrl(),
                storageKey,
                uploaderId
        ));

        return new ImagePresignedUploadResponse(
                image.getId(),
                image.getOriginName(),
                image.getImageName(),
                image.getImageUrl(),
                image.getStorageKey(),
                presignedUpload.uploadUrl(),
                presignedUpload.method(),
                request.contentType(),
                presignedUpload.expiresInSeconds()
        );
    }

    @Override
    @Transactional
    public ImagePresignedUploadResponse createProfileImagePresignedUrl(Long uploaderId, ImagePresignedUploadRequest request) {
        requireUploader(uploaderId);
        validateImageMetadata(request);

        String originName = request.originName();
        String extension = extractExtension(originName);
        String imageName = UUID.randomUUID() + "." + extension;
        String storageKey = createStorageKey("profile-images", imageName);
        ImageStorageService.PresignedUpload presignedUpload = imageStorageService.presignPut(storageKey, request.contentType(), request.fileSize());
        ProfileImage image = profileImageRepository.save(new ProfileImage(
                originName,
                imageName,
                presignedUpload.imageUrl(),
                storageKey,
                uploaderId
        ));

        return new ImagePresignedUploadResponse(
                image.getId(),
                image.getOriginName(),
                image.getImageName(),
                image.getImageUrl(),
                image.getStorageKey(),
                presignedUpload.uploadUrl(),
                presignedUpload.method(),
                request.contentType(),
                presignedUpload.expiresInSeconds()
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
        deferredImageDeletionService.delete(image.getStorageKey());
    }

    private void requireUploader(Long uploaderId) {
        if (uploaderId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
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

        String extension = extractExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        validateContentTypeMatchesExtension(contentType, extension);
        validateMagicBytes(file, extension);
    }

    private void validateImageMetadata(ImagePresignedUploadRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.IMAGE_FILE_REQUIRED);
        }

        if (request.fileSize() == null || request.fileSize() <= 0) {
            throw new CustomException(ErrorCode.IMAGE_FILE_REQUIRED);
        }

        if (request.fileSize() > MAX_IMAGE_SIZE) {
            throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }

        String extension = extractExtension(request.originName());
        validateContentTypeMatchesExtension(request.contentType(), extension);
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

    private void validateContentTypeMatchesExtension(String contentType, String extension) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }

        if (!contentType.equals(expectedContentType(extension))) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }
    }

    private String expectedContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> throw new CustomException(ErrorCode.INVALID_IMAGE_EXTENSION);
        };
    }

    private void validateMagicBytes(MultipartFile file, String extension) {
        byte[] header = readHeader(file);
        boolean valid = switch (extension) {
            case "jpg", "jpeg" -> hasPrefix(header, 0xFF, 0xD8, 0xFF);
            case "png" -> hasPrefix(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> hasAsciiPrefix(header, "GIF87a") || hasAsciiPrefix(header, "GIF89a");
            case "webp" -> hasAsciiPrefix(header, "RIFF")
                    && header.length >= 12
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
            default -> false;
        };

        if (!valid) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
        }
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(MAGIC_BYTES_READ_LIMIT);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED, e);
        }
    }

    private boolean hasPrefix(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }

        return true;
    }

    private boolean hasAsciiPrefix(byte[] bytes, String prefix) {
        if (bytes.length < prefix.length()) {
            return false;
        }

        for (int i = 0; i < prefix.length(); i++) {
            if (bytes[i] != (byte) prefix.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    private String createStorageKey(String directory, String imageName) {
        return directory + "/" + LocalDate.now().format(DATE_PATH_FORMATTER) + "/" + imageName;
    }
}
