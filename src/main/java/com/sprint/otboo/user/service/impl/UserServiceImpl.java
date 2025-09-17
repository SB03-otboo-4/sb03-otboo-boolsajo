package com.sprint.otboo.user.service.impl;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserProfileRepository;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;



    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        validateDuplicateEmail(request.email());
        validateDuplicateUsername(request.name());

        User user = User.builder()
            .username(request.name())
            .password(passwordEncoder.encode(request.password()))
            .email(request.email())
            .role(Role.USER)
            .providerUserId(null)
            .build();

        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public void updatePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 피드백 사항인 새 비밀번호가 기존 비밀번호와 동일한지 검증하는 로직
        if (passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        String encodedNewPassword = passwordEncoder.encode(request.password());
        user.updatePassword(encodedNewPassword);
    }

    @Override
    @Transactional
    public UserDto updateUserLockStatus(UUID userId, UserLockUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateLockStatus(request.locked());
        User savedUser = userRepository.save(user);

        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Role newRole = Role.valueOf(request.role());
        user.updateRole(newRole);
        User savedUser = userRepository.save(user);

        return userMapper.toUserDto(savedUser);
    }

    @Override
    public ProfileDto getUserProfile(UUID userId) {
        UserProfile userProfile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toProfileDto(userProfile.getUser(), userProfile);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            CustomException exception = new CustomException(ErrorCode.DUPLICATE_EMAIL);
            exception.addDetail("email", email);
            throw exception;
        }
    }

    private void validateDuplicateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            CustomException exception = new CustomException(ErrorCode.DUPLICATE_USERNAME);
            exception.addDetail("username", username);
            throw exception;
        }
    }

}

