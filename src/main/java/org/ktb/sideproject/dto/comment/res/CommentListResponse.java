package org.ktb.sideproject.dto.comment.res;

import org.ktb.sideproject.dto.post.PageInfo;

import java.util.List;

public record CommentListResponse(
        List<CommentResponse> commentListResponse,
        PageInfo pageInfo
) {
}
