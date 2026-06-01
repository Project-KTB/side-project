package org.ktb.sideproject.dto.post.res;

import org.ktb.sideproject.entity.Post;

public record PostUpdateResponse(
        Long postId,
        String title,
        String content,
        String imageUrl
) {
    public static PostUpdateResponse from(Post post) {
        return new PostUpdateResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl()
        );
    }
}
