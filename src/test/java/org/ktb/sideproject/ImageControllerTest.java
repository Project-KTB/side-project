package org.ktb.sideproject;

import org.junit.jupiter.api.Test;
import org.ktb.sideproject.controller.ImageController;
import org.ktb.sideproject.dto.image.req.ImagePresignedUploadRequest;
import org.ktb.sideproject.service.ImageService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    @Test
    void postImageUploadPassesAuthenticatedPrincipalToService() {
        ImageService imageService = mock(ImageService.class);
        ImageController controller = new ImageController(imageService);
        ReflectionTestUtils.setField(controller, "imageUploadEnabled", true);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                "dummy".getBytes()
        );

        controller.uploadPostImage(42L, image);

        verify(imageService).uploadPostImage(42L, image);
    }

    @Test
    void profileImageUploadPassesAuthenticatedPrincipalToService() {
        ImageService imageService = mock(ImageService.class);
        ImageController controller = new ImageController(imageService);
        ReflectionTestUtils.setField(controller, "imageUploadEnabled", true);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "dummy".getBytes()
        );

        controller.uploadProfileImage(42L, image);

        verify(imageService).uploadProfileImage(42L, image);
    }

    @Test
    void postPresignedUrlPassesAuthenticatedPrincipalToService() {
        ImageService imageService = mock(ImageService.class);
        ImageController controller = new ImageController(imageService);
        ReflectionTestUtils.setField(controller, "imageUploadEnabled", true);
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest("test.png", "image/png", 1024L);

        controller.createPostImagePresignedUrl(42L, request);

        verify(imageService).createPostImagePresignedUrl(42L, request);
    }

    @Test
    void profilePresignedUrlPassesAuthenticatedPrincipalToService() {
        ImageService imageService = mock(ImageService.class);
        ImageController controller = new ImageController(imageService);
        ReflectionTestUtils.setField(controller, "imageUploadEnabled", true);
        ImagePresignedUploadRequest request = new ImagePresignedUploadRequest("profile.png", "image/png", 1024L);

        controller.createProfileImagePresignedUrl(42L, request);

        verify(imageService).createProfileImagePresignedUrl(42L, request);
    }

    @Test
    void postImageUploadReturnsNotImplementedWhenUploadDisabled() throws Exception {
        ImageService imageService = mock(ImageService.class);
        ImageController controller = new ImageController(imageService);
        ReflectionTestUtils.setField(controller, "imageUploadEnabled", false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/images/posts").file(image))
                .andExpect(status().isNotImplemented());

        verifyNoInteractions(imageService);
    }

    @Test
    void profileImageUploadReturnsNotImplementedWhenUploadDisabled() throws Exception {
        ImageService imageService = mock(ImageService.class);
        ImageController controller = new ImageController(imageService);
        ReflectionTestUtils.setField(controller, "imageUploadEnabled", false);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/images/profile").file(image))
                .andExpect(status().isNotImplemented());

        verifyNoInteractions(imageService);
    }
}
