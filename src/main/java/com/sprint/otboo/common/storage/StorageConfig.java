package com.sprint.otboo.common.storage;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(S3BucketProperties.class)
public class StorageConfig {

    @Bean
    @Qualifier("profileImageStorageService")
    public FileStorageService profileImageStorageService(
        S3Client s3Client,
        S3BucketProperties properties
    ) {
        return new S3FileStorageService(
            s3Client,
            properties.bucket(),
            properties.baseUrl(),
            properties.prefixes().profile()
        );
    }

    @Bean
    @Qualifier("clothingImageStorageService")
    public FileStorageService clothingImageStorageService(
        S3Client s3Client,
        S3BucketProperties properties
    ) {
        return new S3FileStorageService(
            s3Client,
            properties.bucket(),
            properties.baseUrl(),
            properties.prefixes().clothing()
        );
    }
}
