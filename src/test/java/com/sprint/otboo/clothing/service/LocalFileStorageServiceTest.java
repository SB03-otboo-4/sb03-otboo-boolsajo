package com.sprint.otboo.clothing.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sprint.otboo.common.storage.LocalFileStorageService;
import com.sprint.otboo.common.storage.StorageException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("로컬 파일 스토리지 서비스 테스트")
public class LocalFileStorageServiceTest {

    private LocalFileStorageService fileStorageService;

    private Path tempUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        // 임시 업로드 디렉터리 생성( test용 )
        tempUploadDir = Files.createTempDirectory("uploads");

        // 테스트용 정보 주입
        fileStorageService = new LocalFileStorageService(
            tempUploadDir.toAbsolutePath().toString(),
            "http://localhost:8080/uploads"
        );

    }

    @AfterEach
    void tearDown() throws IOException {
        // 테스트 종료 후 임시 디렉터리 및 내부 파일 모두 삭제
        if (tempUploadDir != null && Files.exists(tempUploadDir)) {
            Files.walk(tempUploadDir)
                .map(Path::toFile)
                .sorted((o1, o2) -> -o1.compareTo(o2)) // 파일 먼저 삭제 후 디렉토리 삭제
                .forEach(File::delete);
        }
    }

    @Test
    void upload_null파일이면_빈문자열반환() {
        // given
        MockMultipartFile file = null;

        // when
        String url = fileStorageService.upload(file);

        // then
        assertThat(url).isEmpty();
    }

    @Test
    void upload_정상업로드() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Hello World".getBytes()
        );

        // when
        String url = fileStorageService.upload(file);

        // then
        assertThat(url).startsWith("http://localhost:8080/uploads/");
        assertThat(Files.exists(tempUploadDir.resolve(url.substring(url.lastIndexOf("/") + 1))))
            .isTrue();
    }

    @Test
    void delete_파일존재시_삭제() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "delete_test.txt",
            "text/plain",
            "Delete me".getBytes()
        );

        String url = fileStorageService.upload(file);
        Path uploadedFile = tempUploadDir.resolve(url.substring(url.lastIndexOf("/") + 1));
        assertThat(Files.exists(uploadedFile)).isTrue();

        // when
        fileStorageService.delete(url);

        // then
        assertThat(Files.exists(uploadedFile)).isFalse();
    }

    @Test
    void delete_파일없으면_예외없이_통과() {
        // given
        String url = "http://localhost:8080/uploads/nonexistent.txt";

        // when & then
        assertThatCode(() -> fileStorageService.delete(url)).doesNotThrowAnyException();
    }

    @Test
    void upload_IO예외발생시_FileStorageException() throws IOException {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.txt");

        doThrow(new IOException("강제 IOException")).when(file).transferTo(any(File.class));

        // when & then
        assertThatThrownBy(() -> fileStorageService.upload(file))
            .isInstanceOf(StorageException.class)
            .hasMessageContaining("파일 업로드에 실패했습니다.");
    }

}