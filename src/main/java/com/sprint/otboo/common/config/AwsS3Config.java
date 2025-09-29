package com.sprint.otboo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    @Bean
    @ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "true")
    public S3Client s3Client(@Value("${storage.s3.region}") String region) {
        return S3Client.builder()
            .region(Region.of(region))
            .build();
    }
}
