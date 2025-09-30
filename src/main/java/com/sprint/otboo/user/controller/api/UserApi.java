package com.sprint.otboo.user.controller.api;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.dto.request.ChangePasswordRequest;
import com.sprint.otboo.user.dto.request.ProfileUpdateRequest;
import com.sprint.otboo.user.dto.request.UserCreateRequest;
import com.sprint.otboo.user.dto.request.UserLockUpdateRequest;
import com.sprint.otboo.user.dto.request.UserRoleUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "프로필 관리", description = "프로필 관리 API")
public interface UserApi {

    @Operation(summary = "계정 목록 조회", description = "계정 목록 조회 API")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "계정 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = CursorPageResponse.class))),
        @ApiResponse(responseCode = "400",
            description = "계정 목록 조회 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<CursorPageResponse<UserDto>> listUsers(
        String cursor,
        String idAfter,
        @Min(1) Integer limit,
        @Pattern(regexp = "email|createdAt") String sortBy,
        @Pattern(regexp = "ASCENDING|DESCENDING") String sortDirection,
        String emailLike,
        String roleEqual,
        Boolean locked
    );

    @Operation(summary = "사용자 등록 ( 회원가입 )", description = "사용자 등록 ( 회원 가입 ) API")
    @ApiResponses({
        @ApiResponse(responseCode = "201",
            description = "사용자 등록 ( 회원가입 ) 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400",
            description = "사용자 등록 ( 회원가입 ) 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UserDto> createUser(@Valid UserCreateRequest request);

    @Operation(summary = "권한 수정", description = "권한 수정 API")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "권한 수정 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "400",
            description = "권한 수정 실패 ( 사용자 없음 )",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<UserDto> updateUserRole(UUID userId, @Valid UserRoleUpdateRequest request);

    @Operation(summary = "프로필 조회", description = "프로필 조회 API")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "프로필 업데이트 성공",
            content = @Content(schema = @Schema(implementation = ProfileDto.class))),
        @ApiResponse(responseCode = "400",
            description = "프로필 업데이트 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ProfileDto> getUserProfile(UUID userId);

    @Operation(summary = "프로필 업데이트", description = "프로필 업데이트 API")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "프로필 업데이트 성공",
            content = @Content(schema = @Schema(implementation = ProfileDto.class))),
        @ApiResponse(responseCode = "400",
            description = "프로필 업데이트 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<ProfileDto> updateUserProfile(
        UUID userId,
        CustomUserDetails currentUser,
        @Valid ProfileUpdateRequest request,
        MultipartFile image
    );

    @Operation(summary = "비밀번호 변경", description = "비밀번호를 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "비밀번호 변경 성공"),
        @ApiResponse(responseCode = "400",
            description = "비밀번호 변경 실패 ( 잘못된 요청 )",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404",
            description = "비밀번호 변경 실패 ( 사용자 없음 )",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    ResponseEntity<Void> updatePassword(UUID userId, @Valid ChangePasswordRequest request);

    @Operation(summary = "계정 잠금 상태 변경", description = "[어드민] 계정 잠금/해제를 수행합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "계정 잠금 상태 변경 성공",
            content = @Content(schema = @Schema(implementation = UserDto.class))),
        @ApiResponse(responseCode = "404",
            description = "계정 잠금 상태 변경 실패 ( 사용자 없음 )",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    ResponseEntity<UserDto> updateUserLockStatus(UUID userId, @Valid UserLockUpdateRequest request);
}
