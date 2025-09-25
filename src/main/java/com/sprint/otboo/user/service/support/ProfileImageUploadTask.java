package com.sprint.otboo.user.service.support;

import java.util.UUID;

public record ProfileImageUploadTask(
    UUID userId,
    String originalFilename,
    String contentType,
    byte[] bytes
) {

}
