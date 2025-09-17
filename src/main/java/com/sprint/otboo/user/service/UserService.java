package com.sprint.otboo.user.service;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import java.util.UUID;

public interface UserService {

    UserDto createUser(UserCreateRequest request);

    void updatePassword(UUID userId, ChangePasswordRequest request);

    UserDto updateUserLockStatus(UUID userId, UserLockUpdateRequest request);

    UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request);
}