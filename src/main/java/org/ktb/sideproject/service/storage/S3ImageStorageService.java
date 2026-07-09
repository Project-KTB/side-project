package org.ktb.sideproject.service.storage;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.image-storage.type", havingValue = "s3")
public class S3ImageStorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.image-storage.s3.bucket}")
    private String bucket;

    @Value("${app.image-storage.s3.region}")
    private String region;

    @Value("${app.image-storage.s3.public-url-prefix:}")
    private String publicUrlPrefix;

    @Value("${app.image-storage.s3.presigned-url-duration-seconds:300}")
    private long presignedUrlDurationSeconds;

    @Override
    public StoredImage store(MultipartFile file, String storageKey) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED, e);
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED, e);
        }

        return new StoredImage(storageKey, resolveImageUrl(storageKey));
    }

    @Override
    public PresignedUpload presignPut(String storageKey, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlDurationSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
            return new PresignedUpload(
                    uploadUrl,
                    "PUT",
                    resolveImageUrl(storageKey),
                    presignedUrlDurationSeconds
            );
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.IMAGE_SAVE_FAILED, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.IMAGE_DELETE_FAILED, e);
        }
    }

    @Override
    public String resolveImageUrl(String storageKey) {
        String prefix = imageUrlPrefix();
        if (prefix.isBlank()) {
            return storageKey;
        }

        return prefix + "/" + storageKey;
    }

    @Override
    public String imageUrlPrefix() {
        String prefix = normalizePrefix(publicUrlPrefix);

        if (!prefix.isBlank()) {
            return prefix;
        }

        return "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        return prefix.endsWith("/")
                ? prefix.substring(0, prefix.length() - 1)
                : prefix;
    }
}
