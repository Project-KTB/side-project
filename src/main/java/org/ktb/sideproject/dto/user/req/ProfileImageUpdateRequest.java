package org.ktb.sideproject.dto.user.req;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ProfileImageUpdateRequest(
        String profileImage
) {
}
