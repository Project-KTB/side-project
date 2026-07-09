package org.ktb.sideproject.dto.image.req;

public record ImagePresignedUploadRequest(
        String originName,
        String contentType,
        Long fileSize
) {
}
