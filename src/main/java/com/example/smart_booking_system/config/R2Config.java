package com.smartbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import java.net.URI;

@Configuration
public class R2Config {

    @Value("${r2.accessKeyId}")
    private String accessKey;

    @Value("${r2.secretKey}")
    private String secretKey;

    @Value("${r2.endpoint}")
    private String endpoint;

    @Bean
    public S3Client r2Client() {

        return S3Client.builder()
                .region(Region.US_EAST_1)  // Cloudflare R2 required (fake region)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .endpointOverride(URI.create(endpoint)) // IMPORTANT
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true) // R2 requires this
                                .build()
                )
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }
}
