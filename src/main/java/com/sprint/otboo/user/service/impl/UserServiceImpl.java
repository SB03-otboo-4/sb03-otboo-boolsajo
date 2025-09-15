package com.sprint.otboo.user.service.impl;

import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
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

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다 : " + email);
        }
    }

    private void validateDuplicateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 이름입니다 : " + username);
        }
    }
}

