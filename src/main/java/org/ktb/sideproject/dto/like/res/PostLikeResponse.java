package org.ktb.sideproject.dto.like.res;

public record PostLikeResponse(
        Long postId,
        int likesCount,
        boolean liked
) {
}
