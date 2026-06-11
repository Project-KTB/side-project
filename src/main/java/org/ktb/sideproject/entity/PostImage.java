package org.ktb.sideproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "PostImage",
        indexes = @Index(name = "IDX_POST_IMAGE_POST", columnList = "postId")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "postImageId", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String originName;

    @Column(nullable = false)
    private String imageName;

    @Column(nullable = false, length = 500, unique = true)
    private String imageUrl;

    @Column(nullable = false, length = 500, unique = true)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageStatus status;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId")
    private Post post;

    public PostImage(String originName, String imageName, String imageUrl, String storageKey) {
        this.originName = originName;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.storageKey = storageKey;
        this.status = ImageStatus.PENDING;
    }

    public void attachTo(Post post) {
        this.post = post;
        this.status = ImageStatus.SAVED;
    }

    public void detach() {
        this.post = null;
    }
}
