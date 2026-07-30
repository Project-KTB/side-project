package org.ktb.sideproject.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    StoredImage store(MultipartFile file, String storageKey);

    PresignedUpload presignPut(String storageKey, String contentType, long contentLength);

    void delete(String storageKey);

    String resolveImageUrl(String storageKey);

    String imageUrlPrefix();

    record StoredImage(
            String storageKey,
            String imageUrl
    ) {
    }

    record PresignedUpload(
            String uploadUrl,
            String method,
            String imageUrl,
            long expiresInSeconds
    ) {
    }
}
