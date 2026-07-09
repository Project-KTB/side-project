package org.ktb.sideproject.service.Impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ktb.sideproject.dto.image.req.ImagePresignedUploadRequest;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.PostImageRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.service.storage.ImageStorageService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private ImageServiceImpl imageService;

    @Test
    void uploadPostImageStoresImageThroughConfiguredStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "route.png",
                "image/png",
                "test-image".getBytes()
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

        var response = imageService.uploadPostImage(file);

        assertThat(response.originName()).isEqualTo("route.png");
        assertThat(response.imageName()).endsWith(".png");
        assertThat(response.storageKey()).startsWith("post-images/");
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/" + response.storageKey());

        verify(imageStorageService).store(eq(file), eq(response.storageKey()));
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    void createPostImagePresignedUrlPersistsPendingImageAndReturnsUploadUrl() {
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest(
                "route.webp",
                "image/webp",
                1024L
        );

        when(imageStorageService.presignPut(anyString(), eq("image/webp")))
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

        var response = imageService.createPostImagePresignedUrl(request);

        assertThat(response.originName()).isEqualTo("route.webp");
        assertThat(response.imageName()).endsWith(".webp");
        assertThat(response.storageKey()).startsWith("post-images/");
        assertThat(response.uploadUrl()).isEqualTo("https://s3-presigned.example.com/upload");
        assertThat(response.method()).isEqualTo("PUT");
        assertThat(response.contentType()).isEqualTo("image/webp");
        assertThat(response.expiresInSeconds()).isEqualTo(300);
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/" + response.storageKey());

        verify(imageStorageService).presignPut(eq(response.storageKey()), eq("image/webp"));
        verify(postImageRepository).save(any(PostImage.class));
    }

    @Test
    void createPostImagePresignedUrlRejectsInvalidContentType() {
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest(
                "route.txt",
                "text/plain",
                1024L
        );

        assertThatThrownBy(() -> imageService.createPostImagePresignedUrl(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }
}
