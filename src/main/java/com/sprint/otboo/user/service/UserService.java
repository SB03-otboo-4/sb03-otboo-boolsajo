package com.sprint.otboo.user.service;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.UserCreateRequest;

public interface UserService {

    UserDto createUser(UserCreateRequest request);
}