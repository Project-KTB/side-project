package org.ktb.sideproject.dto.post.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotNull @Size(max=26)
        String title,
        @NotNull
        String content
) {
}
