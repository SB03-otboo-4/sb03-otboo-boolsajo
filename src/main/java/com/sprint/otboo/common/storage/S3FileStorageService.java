package com.sprint.otboo.common.storage;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "true")
public class S3FileStorageService implements FileStorageService{

    private final S3Client s3Client;

    @Value("${storage.s3.bucket}")
    private String bucket;

    @Value("${storage.s3.base-url}")
    private String baseUrl;

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        String key = "binary-content/" + System.currentTimeMillis() + "-" + file.getOriginalFilename();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()
            ));

            return baseUrl + "/" + key;
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new StorageException("S3 파일 업로드에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || url.isBlank()) return;

        String key = url.replace(baseUrl + "/", "");
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        s3Client.deleteObject(request);
    }
}
