package org.ktb.sideproject.dto.image.req;

import org.springframework.web.multipart.MultipartFile;

public record ImageUploadRequest(
        MultipartFile multipartFile
) {
}
