package org.ktb.sideproject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.image-upload.enabled=false",
        "refresh-token.hash-secret=test-refresh-token-hash-secret-which-is-long-enough-for-context-tests",
        "spring.datasource.username=root",
        "spring.datasource.password=1234",
        "jwt.secret=Tm5KRFFUWTBTMWsyTkhWck5qVXlPRFkyWlVrMk5IVnJOMGxMY3pkS1YwVTNTalpSTjB4RGJ6ZE1iVEEzV1U5Qk4xbDVUVGRhVjFrPQ=="
})
@AutoConfigureMockMvc
class ImageUploadSecurityTest {

    private static final String TEST_JWT_SECRET = "Tm5KRFFUWTBTMWsyTkhWck5qVXlPRFkyWlVrMk5IVnJOMGxMY3pkS1YwVTNTalpSTjB4RGJ6ZE1iVEEzV1U5Qk4xbDVUVGRhVjFrPQ==";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void disabledPostImageUploadIsRejectedBySecurityBeforeControllerSuccessPath() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/images/posts").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledProfileImageUploadIsRejectedBySecurityBeforeControllerSuccessPath() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/images/profile").file(image))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledPostPresignedUrlIsRejectedBySecurityBeforeControllerSuccessPath() throws Exception {
        mockMvc.perform(post("/images/posts/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originName\":\"route.png\",\"contentType\":\"image/png\",\"fileSize\":1024}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledProfilePresignedUrlIsRejectedBySecurityBeforeControllerSuccessPath() throws Exception {
        mockMvc.perform(post("/images/profile/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originName\":\"profile.png\",\"contentType\":\"image/png\",\"fileSize\":1024}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUploadedFileServingIsRejectedBySecurity() throws Exception {
        mockMvc.perform(get("/uploads/example.png"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledPostImageUploadIsDeniedForAuthenticatedUser() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/images/posts")
                        .file(image)
                        .header(AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledProfileImageUploadIsDeniedForAuthenticatedUser() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "dummy".getBytes()
        );

        mockMvc.perform(multipart("/images/profile")
                        .file(image)
                        .header(AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledPostPresignedUrlIsDeniedForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/images/posts/presigned-url")
                        .header(AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originName\":\"route.png\",\"contentType\":\"image/png\",\"fileSize\":1024}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledProfilePresignedUrlIsDeniedForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/images/profile/presigned-url")
                        .header(AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originName\":\"profile.png\",\"contentType\":\"image/png\",\"fileSize\":1024}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledUploadedFileServingIsDeniedForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/uploads/example.png")
                        .header(AUTHORIZATION, bearerToken()))
                .andExpect(status().isForbidden());
    }

    private String bearerToken() {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 60_000L);
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("1")
                .claim("type", "accessToken")
                .claim("role", "ROLE_USER")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();

        return "Bearer " + token;
    }
}
