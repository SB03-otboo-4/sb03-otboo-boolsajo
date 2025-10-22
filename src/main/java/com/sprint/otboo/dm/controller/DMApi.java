package com.sprint.otboo.dm.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.dm.dto.response.DirectMessageProtoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
    name = "DirectMessage",
    description = """
        DM(1:1 대화) 조회 및 전송 규격.
        - 조회: REST 커서 페이지네이션 (이 API)
        - 전송: STOMP(WebSocket) `/pub/direct-messages_send` 로 송신, 구독은 `/sub/dm/{sortedKey}`
        - 커서: createdAt(ISO-8601, UTC) + id(UUID) 페어
        """
)
@SecurityRequirement(name = "bearerAuth")
public interface DMApi {

    @Operation(
        summary = "DM 대화 목록 조회 (커서 페이지네이션)",
        description = """
            특정 사용자와의 DM 목록을 최신순으로 조회합니다.
            - 첫 페이지: cursor/idAfter 없이 호출
            - 다음 페이지: 이전 응답의 nextCursor / nextIdAfter 사용
            - 정렬: createdAt desc, (동일시) id desc
            - limit: 1~100 (기본 서비스 로직 기준)
            """,
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "성공",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CursorPageResponse.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 커서/파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
        }
    )
    @GetMapping(value = "/api/direct-messages", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CursorPageResponse<DirectMessageProtoResponse>> getList(
        @Parameter(description = "상대 유저 ID(필수)")
        @RequestParam("userId") UUID otherUserId,

        @Parameter(description = "커서: 직전 응답의 nextCursor(ISO-8601, UTC)", required = false)
        @RequestParam(value = "cursor", required = false) String cursor,

        @Parameter(description = "커서: 직전 응답의 nextIdAfter(UUID). cursor와 페어로 사용", required = false)
        @RequestParam(value = "idAfter", required = false) UUID idAfter,

        @Parameter(description = "가져올 개수(1~100). 생략 시 서비스 기본값 적용", required = false)
        @RequestParam(value = "limit", required = false) Integer limit
    );
}
