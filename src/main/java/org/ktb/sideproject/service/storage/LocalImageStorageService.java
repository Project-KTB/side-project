package org.ktb.sideproject.service.storage;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.image-storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.image-url-prefix}")
    private String imageUrlPrefix;

    @Override
    public StoredImage store(MultipartFile file, String storageKey) {
        Path savePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED, e);
        }

        return new StoredImage(storageKey, resolveImageUrl(storageKey));
    }

    @Override
    public PresignedUpload presignPut(String storageKey, String contentType, long contentLength) {
        throw new CustomException(
                ErrorCode.IMAGE_SAVE_FAILED,
                "Presigned upload is only available when app.image-storage.type=s3."
        );
    }

    @Override
    public void delete(String storageKey) {
        Path imagePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_DELETE_FAILED, e);
        }
    }

    @Override
    public String resolveImageUrl(String storageKey) {
        return imageUrlPrefix() + "/" + storageKey;
    }

    @Override
    public String imageUrlPrefix() {
        return normalizePrefix(imageUrlPrefix);
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
