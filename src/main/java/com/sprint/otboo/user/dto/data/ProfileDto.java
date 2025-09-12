package com.sprint.otboo.user.dto.data;

import com.sprint.otboo.user.entity.Gender;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileDto(
    UUID userId,
    String username,
    String profileImageUrl,
    Gender gender,
    LocalDate birthDate,
    BigDecimal latitude,
    BigDecimal longitude,
    Integer x,
    Integer y,
    String locationNames,
    Integer temperatureSensitivity
) {

}
