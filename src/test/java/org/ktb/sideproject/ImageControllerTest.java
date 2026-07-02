package org.ktb.sideproject;

import org.junit.jupiter.api.Test;
import org.ktb.sideproject.controller.ImageController;
import org.ktb.sideproject.service.ImageService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

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
