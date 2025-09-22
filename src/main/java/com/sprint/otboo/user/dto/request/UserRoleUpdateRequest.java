package com.sprint.otboo.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRoleUpdateRequest(
    @NotBlank(message = "권한은 필수 값입니다.")
    @Pattern(regexp = "^(USER|ADMIN)$", message = "권한은 USER or ADMIN이어야 합니다.")
    String role
) {

}
