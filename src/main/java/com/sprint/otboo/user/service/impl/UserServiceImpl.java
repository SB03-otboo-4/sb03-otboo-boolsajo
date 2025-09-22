package com.sprint.otboo.user.service.impl;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.storage.FileStorageService;
import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.ProfileUpdateRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import com.sprint.otboo.user.entity.Gender;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserProfileRepository;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.repository.query.UserQueryRepository;
import com.sprint.otboo.user.repository.query.UserSlice;
import com.sprint.otboo.user.service.UserService;
import com.sprint.otboo.user.service.support.UserListEnums.SortBy;
import com.sprint.otboo.user.service.support.UserListEnums.SortDirection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserQueryRepository userQueryRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        log.debug("[UserServiceImpl] 회원가입 요청: email = {} ", request.email());
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
        log.debug("[UserServiceImpl] 회원 저장 완료 : userId = {} ", savedUser.getId());

        UserProfile profile = com.sprint.otboo.user.entity.UserProfile.builder()
            .user(savedUser)
            .build();
        userProfileRepository.save(profile);

        log.debug("[UserServiceImpl] 회원가입 성공: userId = {} ", request.email());
        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public void updatePassword(UUID userId, ChangePasswordRequest request) {
        log.debug("[UserServiceImpl] 비밀번호 변경 요청: userId = {} ", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 피드백 사항인 새 비밀번호가 기존 비밀번호와 동일한지 검증하는 로직
        if (passwordEncoder.matches(request.password(), user.getPassword())) {
            log.debug("[UserServiceImpl] 동일한 비밀번호로 인한 예외 발생 : userId = {} ", userId);
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        String encodedNewPassword = passwordEncoder.encode(request.password());
        log.debug("[UserServiceImpl] 비밀번호 변경 성공 : userId = {} ", userId);
        user.updatePassword(encodedNewPassword);
    }

    @Override
    @Transactional
    public UserDto updateUserLockStatus(UUID userId, UserLockUpdateRequest request) {
        log.debug("[UserServiceImpl] 계정 잠금 상태 변경 요청: userId = {} , locked = {} ", userId, request.locked());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateLockStatus(request.locked());
        User savedUser = userRepository.save(user);

        log.debug("[UserServiceImpl] 계정 잠금 상태 변경 완료: userId = {} , locked = {} ", userId, request.locked());
        return userMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        log.debug("[UserServiceImpl] 권한 수정 요청: userId = {} , role = {} ", userId, request.role());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Role newRole = Role.valueOf(request.role());
        user.updateRole(newRole);
        User savedUser = userRepository.save(user);


        log.debug("[UserServiceImpl] 권한 수정 요청: userId = {} , role = {} ", userId, newRole);
        return userMapper.toUserDto(savedUser);
    }

    @Override
    public ProfileDto getUserProfile(UUID userId) {
        log.debug("[UserServiceImpl] 프로필 조회 요청: userId = {} ", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserProfile userProfile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_PROFILE_NOT_FOUND));

        log.debug("[UserServiceImpl] 프로필 조회 성공 : userId = {} ", userId);
        return userMapper.toProfileDto(user, userProfile);
    }

    @Override
    public CursorPageResponse<UserDto> listUsers(String cursor, String idAfter, Integer limit, String sortByParam, String sortDirParam,
        String emailLike, String roleEqualParam, Boolean locked) {
        // 파라미터 -> enum/타입 변환
        SortBy sortBy = SortBy.fromParam(sortByParam);
        SortDirection sd = SortDirection.fromParam(sortDirParam);
        UUID idAfterUuid = (idAfter != null && !idAfter.isBlank()) ? UUID.fromString(idAfter) : null;
        Role roleEqualRole = (roleEqualParam != null && !roleEqualParam.isBlank()) ? Role.valueOf(roleEqualParam) : null;

        // 조회
        UserSlice slice = userQueryRepository.findSlice(cursor, idAfterUuid, limit, sortBy, sd, emailLike, roleEqualRole, locked);
        long total = userQueryRepository.countAll(emailLike, roleEqualRole, locked);

        // 매핑
        List<UserDto> data = slice.rows().stream()
            .map(userMapper::toUserDto)
            .toList();


        return new CursorPageResponse<>(
            data,
            slice.nextCursor(),
            slice.nextIdAfter() != null ? slice.nextIdAfter().toString() : null,
            slice.hasNext(),
            total,
            sortBy.toParam(),
            sd.toParam()
        );
    }

    @Override
    @Transactional
    public ProfileDto updateUserProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_PROFILE_NOT_FOUND));

        user.updateUsername(request.name());

        if (image != null && !image.isEmpty()) {
            if (StringUtils.hasText(user.getProfileImageUrl())) {
                fileStorageService.delete(user.getProfileImageUrl());
            }
            String imageUrl = fileStorageService.upload(image);
            user.updateProfileImageUrl(imageUrl);
        }

        if (StringUtils.hasText(request.gender())) {
            profile.updateGender(Gender.valueOf(request.gender()));
        } else {
            profile.updateGender(null);
        }

        profile.updateBirthDate(request.birthDate());

        if (request.latitude() != null || request.longitude() != null || request.x() != null || request.y() != null
            || request.locationNames() != null) {
            profile.updateLocation(
                request.latitude(),
                request.longitude(),
                request.x(),
                request.y(),
                request.locationNames() == null ? null : String.join(",", request.locationNames())
            );
        }

        if (request.temperatureSensitivity() != null) {
            profile.updateTemperatureSensitivity(request.temperatureSensitivity());
        }

        return userMapper.toProfileDto(user, profile);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("[UserServiceImpl] 중복된 이메일로 인한 예외 발생 email = {}", email);
            CustomException exception = new CustomException(ErrorCode.DUPLICATE_EMAIL);
            exception.addDetail("email", email);
            throw exception;
        }
    }

    private void validateDuplicateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            log.warn("[UserServiceImpl] 중복된 유저이름으로 인한 예외 발생 username = {}", username);
            CustomException exception = new CustomException(ErrorCode.DUPLICATE_USERNAME);
            exception.addDetail("username", username);
            throw exception;
        }
    }
}