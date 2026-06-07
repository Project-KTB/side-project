package org.ktb.sideproject.dto.comment.res;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> commentListResponse
) {
}
