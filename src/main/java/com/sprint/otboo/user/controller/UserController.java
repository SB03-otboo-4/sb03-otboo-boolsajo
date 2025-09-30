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
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import com.sprint.otboo.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
import org.springframework.web.bind.annotation.RequestParam;
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

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("[UserController] 회원가입 요청 : email = {} ", request.email());

        UserDto userDto = userService.createUser(request);

        log.info("[UserController] 회원가입 성공 : userId : {} ", userDto.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

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

    @GetMapping("/{userId}/profiles")
    public ResponseEntity<ProfileDto> getUserProfile(@PathVariable UUID userId) {
        log.info("[UserController] 프로필 조회 요청 : userId = {}", userId);

        ProfileDto profileDto = userService.getUserProfile(userId);

        log.info("[UserController] 프로필 조회 성공 : userId = {}", userId);
        return ResponseEntity.ok(profileDto);
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<UserDto>> listUsers(
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) String idAfter,
        @RequestParam(defaultValue = "20") @Min(1) Integer limit,
        @RequestParam(defaultValue = "createdAt") @Pattern(regexp = "email|createdAt") String sortBy,
        @RequestParam(defaultValue = "DESCENDING") @Pattern(regexp = "ASCENDING|DESCENDING") String sortDirection,
        @RequestParam(required = false) String emailLike,
        @RequestParam(required = false) String roleEqual,
        @RequestParam(required = false) Boolean locked
    ) {
        log.info("[UserController] 계정 목록 조회 요청: cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}, emailLike={}, roleEqual={}, locked={}",
            cursor, idAfter, limit, sortBy, sortDirection, emailLike, roleEqual, locked);

        if (limit == null || limit <= 0) {
            return ResponseEntity.badRequest().build();
        }
        if (!"email".equals(sortBy) && !"createdAt".equals(sortBy)) {
            return ResponseEntity.badRequest().build();
        }
        if (!"ASCENDING".equals(sortDirection) && !"DESCENDING".equals(sortDirection)) {
            return ResponseEntity.badRequest().build();
        }

        CursorPageResponse<UserDto> response = userService.listUsers(cursor, idAfter, limit, sortBy, sortDirection, emailLike, roleEqual, locked);

        log.debug("[UserController] 계정 목록 조회 완료: returnedCount={}, hasNext={}, nextCursor={}",
            response.data().size(), response.hasNext(), response.nextCursor());
        return ResponseEntity.ok(response);
    }

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
