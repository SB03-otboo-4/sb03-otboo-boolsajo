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
import com.sprint.otboo.user.dto.request.UserListQueryParams;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import com.sprint.otboo.user.entity.Gender;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import com.sprint.otboo.user.event.UserLockedEvent;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.user.repository.UserProfileRepository;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.user.repository.query.UserQueryRepository;
import com.sprint.otboo.user.repository.query.UserSlice;
import com.sprint.otboo.user.service.UserService;
import com.sprint.otboo.user.service.support.AsyncProfileImageUploader;
import com.sprint.otboo.user.service.support.ProfileImageUploadTask;
import com.sprint.otboo.user.service.support.UserListEnums.SortBy;
import com.sprint.otboo.user.service.support.UserListEnums.SortDirection;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserQueryRepository userQueryRepository;
    private final FileStorageService fileStorageService;
    private final WeatherLocationQueryService weatherLocationQueryService;
    private final AsyncProfileImageUploader asyncProfileImageUploader;
    private final RetryTemplate profileImageStorageRetryTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * UserService가 의존하는 인프라 클래스를 주입
     *
     * @param userRepository 사용자 엔티티 CRUD 저장소
     * @param userProfileRepository 프로필 엔티티 저장소
     * @param passwordEncoder 비밀번호 인코딩 전략
     * @param userMapper 엔티티-DTO 변환기
     * @param userQueryRepository 커서 기반 조회 전용 리포지토리
     * @param fileStorageService 프로필 이미지 저장소 서비스
     * @param weatherLocationQueryService 좌표→기상 위치 정보 조회 서비스
     * @param asyncProfileImageUploader 비동기 프로필 이미지 후처리기
     * @param profileImageStorageRetryTemplate 이미지 저장 재시도 템플릿
     * @param eventPublisher 계정 상태 변경 이벤트 퍼블리셔
     * */
    public UserServiceImpl(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper,
        UserQueryRepository userQueryRepository,
        @Qualifier("profileImageStorageService") FileStorageService fileStorageService,
        WeatherLocationQueryService weatherLocationQueryService,
        AsyncProfileImageUploader asyncProfileImageUploader,
        @Qualifier("profileImageStorageRetryTemplate") RetryTemplate profileImageStorageRetryTemplate,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userQueryRepository = userQueryRepository;
        this.fileStorageService = fileStorageService;
        this.weatherLocationQueryService = weatherLocationQueryService;
        this.asyncProfileImageUploader = asyncProfileImageUploader;
        this.profileImageStorageRetryTemplate = profileImageStorageRetryTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 신규 사용자를 생성하고 기본 프로필까지 함께 저장
     *
     * @param request 이름,이메일,비밀번호 정보를 담은 가입 요청
     * @return 저장된 사용자 DTO
     * @throws CustomException DUPLICATE_EMAIL, DUPLICATE_USERNAME
     * */
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

        UserProfile profile = UserProfile.builder()
            .user(savedUser)
            .build();
        userProfileRepository.save(profile);

        log.debug("[UserServiceImpl] 회원가입 성공: userId = {} ", request.email());
        return userMapper.toUserDto(savedUser);
    }

    /**
     * 사용자의 로그인 비밀번호를 업데이트
     * 기존 비밀번호와 동일한 값이면 CustomException(SAME_PASSWORD)을 던진다.
     *
     * @param userId 대상 사용자 ID
     * @param request 새 비밀번호 요청
     * @throws CustomException USER_NOT_FOUND, SAME_PASSWORD
     * */
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

    /**
     * 관리자에 의해 계정 잠금 상태를 변경하고, 잠금 시 UserLockedEvent를 발행
     *
     * @param userId 대상 사용자 ID
     * @param request 잠금 여부( true 잠금, false 해제 )
     * @return 변경된 사용자 DTO
     * @throws CustomException USER_NOT_FOUND
     * */
    @Override
    @Transactional
    public UserDto updateUserLockStatus(UUID userId, UserLockUpdateRequest request) {
        log.debug("[UserServiceImpl] 계정 잠금 상태 변경 요청: userId = {} , locked = {} ", userId, request.locked());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateLockStatus(request.locked());
        User savedUser = userRepository.save(user);

        if (request.locked()) {
            log.info("[UserServiceImpl] UserLockedEvent 발행: userId={}", userId);
            eventPublisher.publishEvent(new UserLockedEvent(userId));
        }

        log.debug("[UserServiceImpl] 계정 잠금 상태 변경 완료: userId = {} , locked = {} ", userId, request.locked());
        return userMapper.toUserDto(savedUser);
    }

    /**
     * 사용자의 권한을 변경하고 UserRoleChangedEvent를 발행
     * 이벤트는 권한 변경 후 기존 세션의 무효화를 위해 사용된다.
     *
     * @param userId 대상 사용자 ID
     * @param request 새 Role(USER/ADMIN)
     * @return 변경된 사용자 DTO
     * @throws CustomException USER_NOT_FOUND, IllegalArgumentException( Role 변환 실패 )
     * */
    @Override
    @Transactional
    public UserDto updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        log.debug("[UserServiceImpl] 권한 수정 요청: userId = {} , role = {} ", userId, request.role());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Role previousRole = user.getRole();
        Role newRole = Role.valueOf(request.role());
        user.updateRole(newRole);
        User savedUser = userRepository.save(user);

        log.debug("[UserServiceImpl] publish UserRoleChangedEvent: userId={}, previousRole={}, newRole={}",
            savedUser.getId(), previousRole, newRole);

        eventPublisher.publishEvent(new UserRoleChangedEvent(savedUser.getId(), previousRole, newRole));

        log.debug("[UserServiceImpl] 권한 수정 요청: userId = {} , role = {} ", userId, newRole);
        return userMapper.toUserDto(savedUser);
    }

    /**
     * 사용자와 연결된 프로필 정보를 조회
     *
     * @param userId 프로필 소유자 ID
     * @return 사용자 프로필 DTO
     * @throws CustomException USER_NOT_FOUND, USER_PROFILE_NOT_FOUND
     * */
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

    /**
     * 커서 기반 페이지네이션으로 사용자 목록을 조회
     *
     * @param query 커서·정렬·필터 조건
     * */
    @Override
    public CursorPageResponse<UserDto> listUsers(UserListQueryParams query) {
        log.debug("[UserServiceImpl] 사용자 목록 조회 파라미터: cursor={}, idAfter={}, limit={}, sortBy={}, sortDir={}, emailLike={}, roleEqual={}, locked={}",
            query.cursor(), query.idAfter(), query.limit(), query.sortBy(), query.sortDirection(), query.emailLike(), query.roleEqual(), query.locked());

        // 파라미터 -> enum/타입 변환
        SortBy sortBy = SortBy.fromParam(query.sortBy());
        SortDirection sd = SortDirection.fromParam(query.sortDirection());
        UUID idAfterUuid = query.parsedIdAfter();
        Role roleEqualRole = query.hasRoleEqual() ? Role.valueOf(query.roleEqual()) : null;

        // 조회
        UserSlice slice = userQueryRepository.findSlice(
            query.cursor(), idAfterUuid, query.limit(), sortBy, sd,
            query.emailLike(), roleEqualRole, query.locked()
        );
        long total = userQueryRepository.countAll(query.emailLike(), roleEqualRole, query.locked());

        // 매핑
        List<UserDto> data = slice.rows().stream()
            .map(userMapper::toUserDto)
            .toList();

        log.debug("[UserServiceImpl] 사용자 목록 조회 결과: returnedCount={}, hasNext={}, nextCursor={}",
            data.size(), slice.hasNext(), slice.nextCursor());
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

    /**
     * 프로필 기본 정보와 위치, 이미지 파일을 갱신
     * 이미지가 있으면 기존 이미지를 삭제 후 새 URL로 교체하며 이미지 원본은 비동기로 재업로드한다.
     *
     * @param userId 프로필 소유자 ID
     * @param request 프로필 수정 요청( 이름, 성별, 생일, 위치, 민감도 )
     * @param image 새 프로필 이미지
     * @return 업데이트된 프로필 DTO
     * @throws CustomException USER_NOT_FOUND, USER_PROFILE_NOT_FOUND, FILE_UPLOAD_FAILED
     * */
    @Override
    @Transactional
    public ProfileDto updateUserProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_PROFILE_NOT_FOUND));

        log.debug("[UserServiceImpl] 프로필 업데이트 시작 : userId = {}, hasImage = {}, locationProvided = {} ",
            userId, image != null && !image.isEmpty(), request.location() != null);

        user.updateUsername(request.name());

        if (image != null && !image.isEmpty()) {
            log.debug("[UserServiceImpl] 프로필 이미지 업로드 요청: userId={}, originalFilename={}",
                userId, image.getOriginalFilename());

            if (StringUtils.hasText(user.getProfileImageUrl())) {
                profileImageStorageRetryTemplate.execute(context -> {
                    fileStorageService.delete(user.getProfileImageUrl());
                    return null;
                });
            }
            String imageUrl = profileImageStorageRetryTemplate.execute(
                context -> fileStorageService.upload(image)
            );
            user.updateProfileImageUrl(imageUrl);
        }

        if (StringUtils.hasText(request.gender())) {
            profile.updateGender(Gender.valueOf(request.gender()));
        } else {
            profile.updateGender(null);
        }

        profile.updateBirthDate(request.birthDate());

        if (request.location() != null) {
            log.debug("[UserServiceImpl] 프로필 위치 갱신: userId={}, requestedLat={}, requestedLon={}",
                userId, request.location().latitude(), request.location().longitude());

            WeatherLocationResponse resolved = weatherLocationQueryService.getWeatherLocation(
                request.location().latitude().doubleValue(),
                request.location().longitude().doubleValue()
            );

            List<String> effectiveNames =
                request.location().locationNames().isEmpty()
                    ? resolved.locationNames()
                    : request.location().locationNames();

            profile.updateLocation(
                BigDecimal.valueOf(resolved.latitude()),
                BigDecimal.valueOf(resolved.longitude()),
                resolved.x(),
                resolved.y(),
                effectiveNames.isEmpty() ? null : String.join(" ", effectiveNames)
            );
        }

        if (request.temperatureSensitivity() != null) {
            profile.updateTemperatureSensitivity(request.temperatureSensitivity());
        }

        if (image != null && !image.isEmpty()) {
            try {
                ProfileImageUploadTask task = new ProfileImageUploadTask(
                    userId,
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getBytes()
                );
                asyncProfileImageUploader.upload(task);
            } catch (IOException ex) {
                log.error("[UserServiceImpl] 이미지 데이터를 읽는 중 오류 userId={}", userId, ex);
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, ex);
            }
        }

        log.debug("[UserServiceImpl] 프로필 업데이트 완료 : userId = {} ", userId );
        return userMapper.toProfileDto(user, profile);
    }

    /**
     * 이메일 중복을 검사하고 발견 시 DUPLICATE_EMAIL 예외를 발생
     *
     * @param email 검사할 이메일
     * @throws CustomException DUPLICATE_EMAIL
     * */
    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("[UserServiceImpl] 중복된 이메일로 인한 예외 발생 email = {}", email);
            CustomException exception = new CustomException(ErrorCode.DUPLICATE_EMAIL);
            exception.addDetail("email", email);
            throw exception;
        }
    }

    /**
     * 사용자 이름 중복을 검사하고 발견 시 DUPLICATE_USERNAME 예외를 발생
     *
     * @param username 검사할 사용자명
     * @throws CustomException DUPLICATE_USERNAME
     * */
    private void validateDuplicateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            log.warn("[UserServiceImpl] 중복된 유저이름으로 인한 예외 발생 username = {}", username);
            CustomException exception = new CustomException(ErrorCode.DUPLICATE_USERNAME);
            exception.addDetail("username", username);
            throw exception;
        }
    }
}