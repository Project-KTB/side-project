package org.ktb.sideproject.dto.post.res;

import org.ktb.sideproject.dto.image.ImageInfo;
import org.ktb.sideproject.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

//게시글 상세조회
public record PostDetailResponse(
        Long id,
        String title,
        String content,
        int likesCount,
        int commentsCount,
        int viewsCount,
        LocalDateTime createdAt,
        Long authorId,
        String authorNickname,
        String authorProfileImage,
        Boolean liked,
        // 이미지 리스트 URL
        List<ImageInfo> images
) {
    public static PostDetailResponse from(Post post, Boolean liked, String authorProfileImage) {
        List<ImageInfo> images = post.getImages().stream()
                .map(image -> new ImageInfo(image.getId(), image.getOriginName(), image.getImageUrl()))
                .toList();

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getLikesCount(),
                post.getCommentsCount(),
                post.getViewsCount(),
                post.getCreatedAt(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                authorProfileImage,
                liked,
                images
        );
    }
}
