package com.sprint.otboo.auth.dto;

import com.sprint.otboo.user.dto.data.UserDto;

public record JwtDto(
    UserDto userDto,
    String accessToken
) {

}
