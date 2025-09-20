package com.sprint.otboo.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserLockUpdateRequest(
    @NotNull(message = "잠금 상태는 필수 값입니다.")
    Boolean locked
) {

}
