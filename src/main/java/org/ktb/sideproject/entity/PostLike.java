package org.ktb.sideproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PostLike")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;

    @Builder
    public PostLike(User user, Post post) {
        this.id = new PostLikeId(user.getId(), post.getId());
        this.user = user;
        this.post = post;
    }
}
