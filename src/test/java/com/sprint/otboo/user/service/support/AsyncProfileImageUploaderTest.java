package com.sprint.otboo.user.service.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.otboo.common.storage.FileStorageService;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class AsyncProfileImageUploaderTest {

    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private UserRepository userRepository;
    @Spy
    private RetryTemplate retryTemplate = new RetryTemplate();


    private AsyncProfileImageUploader uploader;

    @BeforeEach
    void setUp() {
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
        retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
        uploader = new AsyncProfileImageUploader(fileStorageService, userRepository, retryTemplate);
    }

    @Test
    void 업로드후_프로필이미지URL_갱신() {
        UUID userId = UUID.randomUUID();
        byte[] bytes = "img".getBytes();
        ProfileImageUploadTask task = new ProfileImageUploadTask(
            userId,
            "file.png",
            "image/png",
            bytes
        );

        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(fileStorageService.upload(any(MultipartFile.class))).willReturn("http://cdn/file.png");

        uploader.upload(task);

        then(fileStorageService).should().upload(any(MultipartFile.class));
        then(user).should().updateProfileImageUrl("http://cdn/file.png");
        then(userRepository).should().save(user);
    }

    @Test
    void 업로드_첫번째실패_두번째성공() {
        UUID userId = UUID.randomUUID();
        byte[] bytes = "img".getBytes();
        ProfileImageUploadTask task = new ProfileImageUploadTask(
            userId, "file.png", "image/png", bytes
        );

        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(fileStorageService.upload(any(MultipartFile.class)))
            .willThrow(new RuntimeException("temporary"))
            .willReturn("http://cdn/file.png");

        uploader.upload(task);

        then(fileStorageService).should(times(2)).upload(any(MultipartFile.class));
        then(user).should().updateProfileImageUrl("http://cdn/file.png");
        then(userRepository).should().save(user);
    }

    @Test
    void 업로드_모든재시도실패() {
        UUID userId = UUID.randomUUID();
        ProfileImageUploadTask task = new ProfileImageUploadTask(
            userId, "file.png", "image/png", "img".getBytes()
        );

        given(fileStorageService.upload(any(MultipartFile.class)))
            .willThrow(new RuntimeException("down"));

        uploader.upload(task);

        then(fileStorageService).should(times(3)).upload(any(MultipartFile.class));
        verifyNoInteractions(userRepository);
    }
}
