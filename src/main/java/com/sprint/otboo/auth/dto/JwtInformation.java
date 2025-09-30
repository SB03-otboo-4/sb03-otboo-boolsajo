package com.sprint.otboo.auth.dto;

import com.sprint.otboo.user.dto.data.UserDto;

public record JwtInformation(
    UserDto userDto,
    String accessToken,
    String refreshToken
) {
}