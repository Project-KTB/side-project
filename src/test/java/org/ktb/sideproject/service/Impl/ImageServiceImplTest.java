package org.ktb.sideproject.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ktb.sideproject.dto.image.req.ImagePresignedUploadRequest;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.PostImageRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.service.storage.DeferredImageDeletionService;
import org.ktb.sideproject.service.storage.ImageStorageService;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private ProfileImageRepository profileImageRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private DeferredImageDeletionService deferredImageDeletionService;

    @InjectMocks
    private ImageServiceImpl imageService;

    @Test
    void uploadPostImageStoresImageThroughConfiguredStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "route.png",
                "image/png",
                pngBytes()
        );

        when(imageStorageService.store(eq(file), anyString()))
                .thenAnswer(invocation -> {
                    String storageKey = invocation.getArgument(1);
                    return new ImageStorageService.StoredImage(
                            storageKey,
                            "https://cdn.example.com/" + storageKey
                    );
                });
        when(postImageRepository.save(any(PostImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = imageService.uploadPostImage(42L, file);

        assertThat(response.originName()).isEqualTo("route.png");
        assertThat(response.imageName()).endsWith(".png");
        assertThat(response.storageKey()).startsWith("post-images/");
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/" + response.storageKey());

        verify(imageStorageService).store(eq(file), eq(response.storageKey()));
        ArgumentCaptor<PostImage> imageCaptor = ArgumentCaptor.forClass(PostImage.class);
        verify(postImageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getUploaderId()).isEqualTo(42L);
    }

    @Test
    void createPostImagePresignedUrlPersistsPendingImageAndReturnsUploadUrl() {
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest(
                "route.webp",
                "image/webp",
                1024L
        );

        when(imageStorageService.presignPut(anyString(), eq("image/webp"), eq(1024L)))
                .thenAnswer(invocation -> {
                    String storageKey = invocation.getArgument(0);
                    return new ImageStorageService.PresignedUpload(
                            "https://s3-presigned.example.com/upload",
                            "PUT",
                            "https://cdn.example.com/" + storageKey,
                            300
                    );
                });
        when(postImageRepository.save(any(PostImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = imageService.createPostImagePresignedUrl(42L, request);

        assertThat(response.originName()).isEqualTo("route.webp");
        assertThat(response.imageName()).endsWith(".webp");
        assertThat(response.storageKey()).startsWith("post-images/");
        assertThat(response.uploadUrl()).isEqualTo("https://s3-presigned.example.com/upload");
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.contentType()).isEqualTo("image/webp");
        assertThat(response.expiresInSeconds()).isEqualTo(300);
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/" + response.storageKey());

        verify(imageStorageService).presignPut(eq(response.storageKey()), eq("image/webp"), eq(1024L));
        ArgumentCaptor<PostImage> imageCaptor = ArgumentCaptor.forClass(PostImage.class);
        verify(postImageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getUploaderId()).isEqualTo(42L);
    }

    @Test
    void uploadPostImageRejectsContentWithoutMatchingMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "route.png",
                "image/png",
                "not-a-real-png".getBytes()
        );

        assertThatThrownBy(() -> imageService.uploadPostImage(42L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    @Test
    void uploadPostImageRejectsContentTypeThatDoesNotMatchExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "route.png",
                "image/jpeg",
                pngBytes()
        );

        assertThatThrownBy(() -> imageService.uploadPostImage(42L, file))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    @Test
    void createPostImagePresignedUrlRejectsOversizedMetadata() {
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest(
                "route.png",
                "image/png",
                5 * 1024 * 1024L + 1
        );

        assertThatThrownBy(() -> imageService.createPostImagePresignedUrl(42L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_SIZE_EXCEEDED);
    }

    @Test
    void createPostImagePresignedUrlRejectsMismatchedContentTypeMetadata() {
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest(
                "route.png",
                "image/jpeg",
                1024L
        );

        assertThatThrownBy(() -> imageService.createPostImagePresignedUrl(42L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    @Test
    void createPostImagePresignedUrlRejectsInvalidContentType() {
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest(
                "route.png",
                "text/plain",
                1024L
        );

        assertThatThrownBy(() -> imageService.createPostImagePresignedUrl(42L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    @Test
    void deletePostImageRoutesStorageKeyThroughDeferredDeletionService() {
        Long userId = 1L;
        Long imageId = 20L;
        User user = User.builder()
                .email("owner@example.com")
                .password("encoded")
                .nickname("owner")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        Post post = Post.builder()
                .title("title")
                .content("content")
                .user(user)
                .build();
        PostImage image = new PostImage(
                "old.png",
                "old.png",
                "/uploads/post-images/old.png",
                "post-images/old.png",
                userId
        );
        ReflectionTestUtils.setField(image, "id", imageId);
        post.addImage(image);

        when(postImageRepository.findById(imageId)).thenReturn(java.util.Optional.of(image));

        imageService.deletePostImage(userId, imageId);

        verify(postImageRepository).delete(image);
        verify(deferredImageDeletionService).delete("post-images/old.png");
        verify(imageStorageService, never()).delete("post-images/old.png");
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
    }
}
