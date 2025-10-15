package com.sprint.otboo.user.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.storage.InMemoryMultipartFile;
import com.sprint.otboo.user.controller.api.UserApi;
import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.ProfileUpdateRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserListQueryParams;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import com.sprint.otboo.user.service.UserService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController implements UserApi {

    private final UserService userService;

    /**
     * 회원가입 요청을 받아 새로운 사용자 생성
     *
     * @param request 이름,이메일,비밀번호를 포함한 가입 정보
     * @return 생성된 사용자 DTO와 201 상태 코드
     * */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("[UserController] 회원가입 요청 : email = {} ", request.email());

        UserDto userDto = userService.createUser(request);

        log.info("[UserController] 회원가입 성공 : userId : {} ", userDto.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    /**
     * 지정된 사용자의 로그인 비밀번호를 업데이트
     *
     * @param userId 대상 사용자 식별자
     * @param request 새로운 비밀번호 ( 유효성 검증 포함 )
     * @return 작업 성공 시 204 No Content
     * */
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
        @PathVariable UUID userId,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("[UserController] 비밀번호 변경 요청 : userId = {} ", userId);

        userService.updatePassword(userId, request);

        log.info("[UserController] 비밀번호 변경 성공 : userId = {} ", userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관리자 권한으로 계정 잠금 여부를 변경
     *
     * @param userId 대상 사용자
     * @param request 잠금 여부 플래그
     * @return 변경된 사용자 DTO
     * */
    @PatchMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserLockStatus(
        @PathVariable UUID userId,
        @Valid @RequestBody UserLockUpdateRequest request
    ) {
        log.info("[UserController] 계정 잠금 상태 변경 요청 : userId = {}, locked = {}", userId, request.locked());

        UserDto updatedUser = userService.updateUserLockStatus(userId, request);

        log.info("[UserController] 계정 잠금 상태 변경 성공 : userId = {}, locked = {}", userId, request.locked());
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 관리자 권한으로 사용자의 Role을 업데이트
     *
     * @param userId 대상 사용자
     * @param request 새로운 역할
     * @return 변경된 사용자 DTO
     * */
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(
        @PathVariable UUID userId,
        @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        log.info("[UserController] 권한 수정 요청 : userId = {}, locked = {}", userId, request.role());

        UserDto updatedUser = userService.updateUserRole(userId, request);

        log.info("[UserController] 권한 수정 성공 : userId = {} ", userId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 주어진 사용자의 프로필 정보를 조회
     *
     * @param userId 프로필 소유자
     * @return 프로필 DTO*/
    @GetMapping("/{userId}/profiles")
    public ResponseEntity<ProfileDto> getUserProfile(@PathVariable UUID userId) {
        log.info("[UserController] 프로필 조회 요청 : userId = {}", userId);

        ProfileDto profileDto = userService.getUserProfile(userId);

        log.info("[UserController] 프로필 조회 성공 : userId = {}", userId);
        return ResponseEntity.ok(profileDto);
    }

    /**
     * 커서 기반 검색 조건으로 사용자 목록을 조회
     *
     * @param query 커서·정렬·필터 조건
     * */
    @GetMapping
    public ResponseEntity<CursorPageResponse<UserDto>> listUsers(@Valid UserListQueryParams query) {
        log.info("[UserController] 계정 목록 조회 요청: {}",query);

        UserListQueryParams resolved = query.withDefaults();
        CursorPageResponse<UserDto> response = userService.listUsers(resolved);

        log.debug("[UserController] 계정 목록 조회 완료: returnedCount={}, hasNext={}, nextCursor={}",
            response.data().size(), response.hasNext(), response.nextCursor());
        return ResponseEntity.ok(response);
    }

    /**
     * 본인 또는 관리자 권한으로 프로필 및 이미지 파일을 수정
     *
     * @param userId 대상 사용자
     * @param currentUser 인증된 사용자 정보
     * @param request 프로필 변경 값
     * @param image 선택적 프로필 이미지 파일
     * @return 수정된 프로필 DTO
     * */
    @PatchMapping(value = "/{userId}/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileDto> updateUserProfile(
        @PathVariable UUID userId,
        @AuthenticationPrincipal CustomUserDetails currentUser,
        @RequestPart("request") @Valid ProfileUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        verifyProfileOwnerOrAdmin(userId, currentUser);
        log.info("[UserController] 프로필 수정 요청: userId={}, hasImage={}",
            userId, image != null && !image.isEmpty());

        MultipartFile imageCopy = null;
        if (image != null && !image.isEmpty()) {
            try {
                imageCopy = new InMemoryMultipartFile(
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getBytes()
                );
            } catch (IOException ex) {
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED, ex);
            }
        }

        ProfileDto profileDto = userService.updateUserProfile(userId, request, imageCopy);
        log.debug("[UserController] 프로필 수정 성공: userId={}, name={}",
            profileDto.userId(), profileDto.name());
        return ResponseEntity.ok(profileDto);
    }

    /**
     * 요청자가 본인 또는 관리자인지에 대한 권한 검증
     *
     * @param userId 대상 사용자
     * @param currentUser 인증 정보
     * */
    private void verifyProfileOwnerOrAdmin(UUID userId, CustomUserDetails currentUser) {
        if (currentUser == null) {
            log.warn("[UserController] 인증 사용자 없음: targetUserId={}", userId);
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        boolean sameUser = userId.equals(currentUser.getUserId());
        boolean isAdmin = currentUser.getAuthorities().stream()
            .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        if (!sameUser && !isAdmin) {
            log.warn("[UserController] 프로필 수정 권한 없음: targetUserId={}, requesterId={}, authorities={}",
                userId, currentUser.getUserId(), currentUser.getAuthorities());
            throw new AccessDeniedException("해당 프로필을 수정할 권한이 없습니다.");
        }

        log.debug("[UserController] 프로필 수정 권한 확인 완료: targetUserId={}, requesterId={}",
            userId, currentUser.getUserId());
    }
}
