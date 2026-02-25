package com.example.smart_booking_system.service;

import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.InternalServerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final S3Client r2Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final String publicDomain;
    private final String endpoint; // Lưu lại endpoint để dùng nếu cần check

    // Allowed extensions
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "png", "jpg", "jpeg", "gif", "bmp", "webp", "heic", "heif"
    );

    public FileStorageService(
            @Value("${r2.accessKeyId}") String accessKey,
            @Value("${r2.secretKey}") String secretKey,
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.bucket}") String bucketName,
            @Value("${r2.publicDomain}") String publicDomain
    ) {
        this.bucket = bucketName;
        this.publicDomain = publicDomain.endsWith("/") ? publicDomain.substring(0, publicDomain.length() - 1) : publicDomain;
        this.endpoint = endpoint;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.r2Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    private boolean isImageFile(MultipartFile file) {
        if (file.getOriginalFilename() == null) return false;
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return ext != null && ALLOWED_IMAGE_EXTENSIONS.contains(ext.toLowerCase());
    }

    // ===========================================
    // ⬆ UPLOAD FILE
    // ===========================================
    public String storeImageFile(MultipartFile file, String subDirectory) {
        try {
            if (file.isEmpty()) throw new BadRequestException("Empty file");
            if (!isImageFile(file)) throw new BadRequestException("Invalid file type");

            String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

            // Tạo key: ví dụ "properties/abc@gmail.com/xyz.jpg"
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

            // Trả về Full Public URL để lưu DB
            return publicDomain + "/" + key;

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
    public void deleteFile(String fileUrlOrKey) {
        if (fileUrlOrKey == null || fileUrlOrKey.trim().isEmpty()) return;

        // [FIX QUAN TRỌNG] Luôn trích xuất key trước khi xóa
        String key = extractKey(fileUrlOrKey);

        try {
            r2Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
        } catch (Exception ignored) {
            // Log warning if needed
        }
    }

    // ===========================================
    // 🔐 SIGNED URL / PUBLIC URL
    // ===========================================

    /**
     * Hàm này sửa lỗi Double URL:
     * Nếu đầu vào là URL -> Cắt lấy key -> Tạo Signed URL chuẩn.
     * Nếu đầu vào là Key -> Tạo Signed URL chuẩn.
     */
    public String generateSignedUrl(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isEmpty()) return null;

        try {
            // [FIX QUAN TRỌNG] Trích xuất key đúng, kể cả khi nằm trong folder con
            String key = extractKey(urlOrKey);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(60)) // Tăng lên 60p cho thoải mái
                    .getObjectRequest(getObjectRequest)
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not generate signed URL for: " + urlOrKey);
        }
    }

    /**
     * Hàm tiện ích để lấy URL sạch (Không ký)
     * Dùng cho trường hợp file public
     */
    public String getPublicUrl(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isEmpty()) return null;
        if (urlOrKey.startsWith("http")) return urlOrKey; // Đã là URL thì trả về luôn
        return publicDomain + "/" + urlOrKey;
    }

    // ===========================================
    // 🛠 UTILS (CORE FIX)
    // ===========================================

    /**
     * Tách Key từ Full URL.
     * Ví dụ Input: "https://pub-domain.r2.dev/properties/user@test.com/img.jpg"
     * Output: "properties/user@test.com/img.jpg"
     * -> Giúp S3Client tìm đúng file trong folder con.
     */
    private String extractKey(String urlOrKey) {
        if (urlOrKey == null) return null;

        // Nếu không phải link http, coi như nó là key
        if (!urlOrKey.startsWith("http")) {
            return urlOrKey;
        }

        // Nếu là link public domain, cắt bỏ phần domain
        if (urlOrKey.startsWith(publicDomain)) {
            // +1 để bỏ dấu "/"
            return urlOrKey.substring(publicDomain.length() + 1);
        }

        // Trường hợp URL lạ (ví dụ link cũ từ domain khác), cố gắng lấy phần path sau domain
        try {
            URI uri = URI.create(urlOrKey);
            String path = uri.getPath();
            // path sẽ là "/properties/..." -> bỏ dấu "/" đầu
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            return urlOrKey; // Fallback
        }
    }
}