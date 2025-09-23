package com.sprint.otboo.user.service.support;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.storage.FileStorageService;
import com.sprint.otboo.common.storage.InMemoryMultipartFile;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


@Component
@Slf4j
public class AsyncProfileImageUploader {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    public AsyncProfileImageUploader(
        @Qualifier("profileImageStorageService") FileStorageService fileStorageService,
        UserRepository userRepository
    ) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
    }

    @Async("fileUploadExecutor")
    public void upload(ProfileImageUploadTask task) {
        if (task.bytes() == null || task.bytes().length == 0) {
            return;
        }

        MultipartFile resource = new InMemoryMultipartFile(
            task.originalFilename(),
            task.contentType(),
            task.bytes()
        );

        try {
            String imageUrl = fileStorageService.upload(resource);
            updateUserProfileImage(task.userId(), imageUrl);
        } catch (Exception ex) {
            log.error("[AsyncProfileImageUploader] 업로드 실패 userId={}", task.userId(), ex);
            // 실패 시 재시도/알림이 필요하다면 여기서 처리
        }
    }

    private void updateUserProfileImage(UUID userId, String imageUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfileImageUrl(imageUrl);
        userRepository.save(user);
        log.info("[AsyncProfileImageUploader] 업로드 완료 userId  = {} ", userId);
    }
}
