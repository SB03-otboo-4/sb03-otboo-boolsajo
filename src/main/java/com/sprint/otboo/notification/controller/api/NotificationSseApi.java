package com.sprint.otboo.notification.controller.api;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE 구독", description = "실시간 알림 SSE 구독 관련 API")
@RequestMapping("/api/sse")
public interface NotificationSseApi {

    @Operation(
        summary = "SSE 구독",
        description = "인증 사용자가 실시간 알림을 구독합니다. LastEventId를 제공하면 누락된 알림 전송 가능"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "SSE 연결 성공",
            content = @Content(
                mediaType = "text/event-stream",
                schema = @Schema(implementation = SseEmitter.class),
                examples = @ExampleObject(value = """
                {
                  "timeout": 9007199254740991
                }
                """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "SSE 구독 실패",
            content = @Content(
                mediaType = "*/*",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
        @Parameter(description = "마지막으로 받은 이벤트 ID", required = false)
        @RequestParam(value = "LastEventId", required = false) String lastEventId
    );
}