package com.example.smart_booking_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class R2Config {

    @Value("${r2.access-key-id}")
    private String accessKeyId;

    @Value("${r2.secret-access-key}")
    private String secretAccessKey;

    // Sửa: Đọc endpoint trực tiếp thay vì accountId
    @Value("${r2.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        // Cấu hình S3 Client để tương thích với R2
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true) // Quan trọng cho R2
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint)) // Sử dụng endpoint từ .env
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.US_EAST_1) // R2 luôn dùng region này (hoặc 'auto')
                .serviceConfiguration(serviceConfiguration)
                .build();
    }
}