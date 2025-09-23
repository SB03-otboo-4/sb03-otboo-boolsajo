package com.sprint.otboo.common.storage;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String baseUrl;
    private final String keyPrefix;

    public S3FileStorageService(S3Client s3Client, String bucket, String baseUrl, String keyPrefix) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.keyPrefix = keyPrefix.replaceAll("^/|/$", "");
    }


    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
            ? file.getOriginalFilename()
            : "upload.bin";

        String key = keyPrefix + "/" + System.currentTimeMillis() + "-" + originalName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(
                request,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return baseUrl + "/" + key;
        } catch (IOException ex) {
            log.error("S3 업로드 실패", ex);
            throw new StorageException("S3 업로드 중 오류가 발생했습니다.", ex);
        }
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith(baseUrl)) {
            return;
        }

        String key = url.substring(baseUrl.length() + 1);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        s3Client.deleteObject(request);
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String trimSlashes(String value) {
        String trimmed = value;
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
