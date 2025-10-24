package com.sprint.otboo.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 255, message = "새 비밀번호는 8자 이상 255자 이하여야 합니다")
    String password
) {

}
