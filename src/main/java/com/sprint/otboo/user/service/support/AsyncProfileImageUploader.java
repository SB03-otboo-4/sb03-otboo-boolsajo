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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


@Component
@Slf4j
public class AsyncProfileImageUploader {

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RetryTemplate profileImageStorageRetryTemplate;

    public AsyncProfileImageUploader(
        @Qualifier("profileImageStorageService") FileStorageService fileStorageService,
        UserRepository userRepository,
        @Qualifier("profileImageStorageRetryTemplate") RetryTemplate profileImageStorageRetryTemplate
    ) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.profileImageStorageRetryTemplate = profileImageStorageRetryTemplate;
    }

    /**
     * 프로필 이미지 업로드 작업을 실행
     * - 바이트가 비어 있으면 아무것도 하지 않는다.
     * - RetryTemplate를 이용하여 업로드를 재시도하며, 재시도 후에도 실패하면 예외를 전파
     *
     * @param task 사용자 ID/파일 메타데이터/바이트를 포함한 업로드 작업
     * */
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
            String imageUrl = profileImageStorageRetryTemplate.execute(
                ctx -> fileStorageService.upload(resource),
                ctx -> {
                    Throwable last = ctx.getLastThrowable();
                    log.error("[AsyncProfileImageUploader] 프로필 이미지 업로드가 {}회 시도 후에도 실패했습니다. userId={}",
                        ctx.getRetryCount(), task.userId(), last);
                    if (last instanceof Exception ex) {
                        throw ex;
                    }
                    throw new RuntimeException(last);
                }
            );
            updateUserProfileImage(task.userId(), imageUrl);
        } catch (Exception ex) {
            log.error("[AsyncProfileImageUploader] 업로드 실패 userId={}", task.userId(), ex);
            // 실패 시 재시도/알림이 필요하다면 여기서 처리
        }
    }

    /**
     * 업로드에 성공한 이미지 URL을 사용자 엔티티에 반영
     *
     * @param userId 이미지 소유 사용자
     * @param imageUrl 저장된 이미지 URL
     * @throws CustomException USER_NOT_FOUND
     * */
    private void updateUserProfileImage(UUID userId, String imageUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateProfileImageUrl(imageUrl);
        userRepository.save(user);
        log.info("[AsyncProfileImageUploader] 업로드 완료 userId  = {} ", userId);
    }
}
