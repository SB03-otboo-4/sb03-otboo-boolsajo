package com.sprint.otboo.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "storage.local.enabled", havingValue = "true", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService{

    private final String uploadDir;
    private final String baseUrl;

    public LocalFileStorageService(
        @Value("${storage.local.upload-dir:/uploads}") String uploadDir,
        @Value("${storage.local.base-url:http://localhost:8080/uploads}") String baseUrl
    ) {
        this.uploadDir = uploadDir;
        this.baseUrl = baseUrl;
    }

    /**
     * 파일을 저장하고 접근 가능한 URL 반환*/
    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }

        try {
            Path dir = ensureUploadDirExists();
            Path saved = saveFile(file, dir);
            return toFileUrl(saved);
        } catch (IOException e) {
            throw new StorageException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * URL에 해당하는 파일을 삭제
     * */
    @Override
    public void delete(String url) {
        if (url == null  || url.isBlank()) {
            return;
        }

        if (!url.startsWith(baseUrl)) {
            return;
        }

        try {
            Path filePath = extractPathFromUrl(url);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new StorageException("파일 삭제에 실패했습니다: " + url, e);
        }
    }

    /**
     * 업로드 디렉터리가 존재하지 않으면 생성 - 절대 경로로 고정
     * */
    private Path ensureUploadDirExists() throws IOException {
        Path dir = Paths.get(uploadDir)
            .toAbsolutePath()
            .normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    /**
     * 실제 파일을 디스크에 저장
     * */
    private Path saveFile(MultipartFile file, Path dir) throws IOException {
        String filename = System.currentTimeMillis() + "-" + file.getOriginalFilename();
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        return target;
    }

    /**
     * 저장된 파일 경로를 외부에서 접근 가능한 URL로 변환
     * */
    private String toFileUrl(Path filePath) {
        return baseUrl + "/" + filePath.getFileName();
    }

    /**
     * URL에서 실제 파일 시스템 경로를 추출
     * */
    private Path extractPathFromUrl(String url) {
        String filename = url.replace(baseUrl + "/", "");
        return Path.of(uploadDir, filename);
    }
}
