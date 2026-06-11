package org.ktb.sideproject.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ProfileImage",
        indexes = @Index(name = "IDX_PROFILE_IMAGE_USER", columnList = "userId")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profileImageId", nullable = false)
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", unique = true)
    private User user;

    public ProfileImage(String originName, String imageName, String imageUrl, String storageKey) {
        this.originName = originName;
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.storageKey = storageKey;
        this.status = ImageStatus.PENDING;
    }

    public void attachTo(User user) {
        this.user = user;
        this.status = ImageStatus.SAVED;
    }
}
