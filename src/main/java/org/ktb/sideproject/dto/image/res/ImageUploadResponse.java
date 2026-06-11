package org.ktb.sideproject.dto.image.res;

public record ImageUploadResponse(
        Long imageId,
        String originName,
        String imageName,
        String imageUrl,
        String storageKey
) {
}
