package com.sprint.otboo.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "storage.s3")
public record S3BucketProperties(
    String bucket,
    String baseUrl,
    @DefaultValue Prefixes prefixes
) {

    public record Prefixes(
        String profile,
        String clothing
    ) {}
}
