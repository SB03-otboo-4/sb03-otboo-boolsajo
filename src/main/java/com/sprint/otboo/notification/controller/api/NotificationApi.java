package com.sprint.otboo.notification.controller.api;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "알림", description = "알림 API")
public interface NotificationApi {


    @Operation(summary = "알림 목록 조회",description = "알림 목록 조회 API")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "알림 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = CursorPageResponse.class))),
        @ApiResponse(responseCode = "400",
            description = "알림 목록 조회 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    CursorPageResponse<NotificationDto> listNotifications(
        @Parameter(hidden = true)CustomUserDetails principal,
        @ParameterObject @Valid NotificationQueryParams query
    );


    @Operation(summary = "알림 읽음 처리", description = "알림 읽음 처리 API")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "알림 읽음 처리 성공"),
        @ApiResponse(responseCode = "400",
            description = "알림 읽음 처리 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> deleteNotification(UUID notificationId);
}
