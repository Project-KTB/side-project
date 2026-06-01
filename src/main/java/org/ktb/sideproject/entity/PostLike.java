package org.ktb.sideproject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(
        name = "PostLike",
        uniqueConstraints = @UniqueConstraint(
                name = "IDX_LIKE_USER_POST",
                columnNames = {"userId", "postId"} // 유니크 인덱스 반영
        )
)
@Getter
@RequiredArgsConstructor
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likeId", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;
}
