package org.ktb.sideproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "image",
        indexes = @Index(name = "IDX_IMAGE_POST", columnList = "postId") // 인덱스 반영
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imageId", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String originName;

    @Column(nullable = false)
    private String imageName;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;

    public Image(String originName, String imageName, String imageUrl, Post post) {
        this.originName = originName;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.post = post;
    }
}
