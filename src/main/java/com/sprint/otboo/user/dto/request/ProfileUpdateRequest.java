package com.sprint.otboo.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProfileUpdateRequest(
    @NotBlank(message = "이름은 필수값입니다.") String name,
    @Size(max = 10, message = "성별은 10자 이하로 입력하세요.") String gender,
    LocalDate birthDate,
    @Valid ProfileLocationUpdateRequest location,
    @Min(value = 0, message = "온도 민감도는 0 이상이어야 합니다.")
    @Max(value = 5, message = "온도 민감도는 5 이하여야 합니다.")
    Integer temperatureSensitivity
) {

}
