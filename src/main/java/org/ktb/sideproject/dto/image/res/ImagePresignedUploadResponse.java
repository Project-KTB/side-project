package org.ktb.sideproject.dto.image.res;

public record ImagePresignedUploadResponse(
        Long imageId,
        String originName,
        String imageName,
        String imageUrl,
        String storageKey,
        String uploadUrl,
        String method,
        String contentType,
        long expiresInSeconds
) {
}
