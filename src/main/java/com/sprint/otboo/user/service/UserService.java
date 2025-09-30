package com.sprint.otboo.user.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.ProfileUpdateRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserDto createUser(UserCreateRequest request);

    void updatePassword(UUID userId, ChangePasswordRequest request);

    UserDto updateUserLockStatus(UUID userId, UserLockUpdateRequest request);

    UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request);

    ProfileDto getUserProfile(UUID userId);

    CursorPageResponse<UserDto> listUsers(String cursor, String idAfter, Integer limit, String sortBy, String sortDirection, String emailLike, String roleEqual, Boolean locked);

    ProfileDto updateUserProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image);
}