package com.sprint.otboo.user.dto.data;

import com.sprint.otboo.user.entity.Gender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProfileDto(
    UUID userId,
    String name,
    String profileImageUrl,
    Gender gender,
    LocalDate birthDate,
    ProfileLocationDto location,
    Integer temperatureSensitivity
) {

}
