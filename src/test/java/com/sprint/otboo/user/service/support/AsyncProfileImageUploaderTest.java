package com.sprint.otboo.user.service.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.sprint.otboo.common.storage.FileStorageService;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class AsyncProfileImageUploaderTest {

    @Mock
    FileStorageService fileStorageService;
    @Mock
    UserRepository userRepository;

    AsyncProfileImageUploader uploader;

    @BeforeEach
    void setUp() {
        uploader = new AsyncProfileImageUploader(fileStorageService, userRepository);
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
}
