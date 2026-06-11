package org.ktb.sideproject.dto.post.res;

import org.ktb.sideproject.dto.image.ImageInfo;
import org.ktb.sideproject.entity.Post;

import java.util.List;

public record PostUpdateResponse(
        Long postId,
        String title,
        String content,
        List<ImageInfo> images
) {
    public static PostUpdateResponse from(Post post) {
        List<ImageInfo> images = post.getImages().stream()
                .map(image -> new ImageInfo(image.getId(), image.getOriginName(), image.getImageUrl()))
                .toList();

        return new PostUpdateResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                images
        );
    }
}
