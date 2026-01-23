package com.example.smart_booking_system.service;

import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.InternalServerException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final S3Client r2Client;
    private final S3Presigner presigner;

    @Value("${r2.bucket}")
    private String bucket;

    // Allowed extensions
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "heic", "heif"
    );

    // Constructor
    public FileStorageService(
            @Value("${r2.accessKeyId}") String accessKey,
            @Value("${r2.secretKey}") String secretKey,
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.bucket}") String bucketName
    ) {
        this.bucket = bucketName;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.r2Client = S3Client.builder()
                .endpointOverride(java.net.URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(java.net.URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private boolean isImageFile(MultipartFile file) {
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        return ext != null && ALLOWED_IMAGE_EXTENSIONS.contains(ext.toLowerCase());
    }

    // ===========================================
    // ⬆ UPLOAD FILE
    // ===========================================
    public String storeImageFile(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) throw new BadRequestException("Empty file");
            if (!isImageFile(file)) throw new BadRequestException("Invalid file type");

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + "." + extension;

            String key = (subDirectory == null || subDirectory.isEmpty())
                    ? fileName
                    : subDirectory + "/" + fileName;

            r2Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );

            return key;

        } catch (IOException e) {
            throw new InternalServerException("Upload failed: " + e.getMessage());
        }
    }

    public String storeImageFile(MultipartFile file) {
        return storeImageFile(file, "");
    }

    // ===========================================
    // 🗑 DELETE FILE
    // ===========================================
    public void deleteFile(String key) {
        if (key == null || key.trim().isEmpty()) return;

        try {
            r2Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
        } catch (Exception ignored) {}
    }

    // ===========================================
    // 🔐 SIGNED URL
    // ===========================================
    public String generateSignedUrl(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30))
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception e) {
            throw new RuntimeException("Could not generate signed URL");
        }
    }
}
