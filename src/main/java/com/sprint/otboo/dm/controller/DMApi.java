package com.sprint.otboo.dm.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.dm.dto.response.DirectMessageProtoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(
    name = "DirectMessage",
    description = """
        1:1 DM 조회 API.
        - 엔드포인트: GET /api/direct-messages
        - 커서: createdAt(ISO-8601) + id(UUID) 페어
        - 정렬: createdAt DESC, 동일시 id DESC
        - 전송은 STOMP 사용: /pub/direct-messages_send
        """
)
@SecurityRequirement(name = "bearerAuth")
public interface DMApi {

    @Operation(
        summary = "DM 목록 조회 (커서 페이지네이션)",
        description = """
            상대 사용자와의 DM을 최신순으로 조회합니다.
            검증:
            - 자기 자신(userId == me) 금지 → SELF_DM_NOT_ALLOWED
            - cursor ISO-8601 형식 → INVALID_CURSOR_FORMAT
            - idAfter는 cursor와 함께 → INVALID_CURSOR_PAIR
            - limit 1~100 → INVALID_PAGING_LIMIT
            """,
        responses = {
            @ApiResponse(responseCode = "200", description = "성공",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CursorPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
        }
    )
    ResponseEntity<CursorPageResponse<DirectMessageProtoResponse>> getList(
        @Parameter(in = ParameterIn.QUERY, required = true,
            description = "상대 사용자 ID(UUID). 자기 자신과 동일하면 400",
            example = "5f1a0d5e-45a0-4c9d-a2f0-0b9c1e5a7e22")
        UUID otherUserId,

        @Parameter(in = ParameterIn.QUERY, required = false,
            description = "커서: 이전 응답의 nextCursor(ISO-8601, UTC)",
            example = "2025-10-12T09:12:34Z")
        String cursor,

        @Parameter(in = ParameterIn.QUERY, required = false,
            description = "커서: 이전 응답의 nextIdAfter(UUID). cursor와 반드시 함께",
            example = "7d9a3c69-3b8e-4a1e-8b6f-3b4d19f71dcd")
        UUID idAfter,

        @Parameter(in = ParameterIn.QUERY, required = false,
            description = "가져올 개수(1~100). 미지정 시 서비스 기본값 사용",
            example = "20")
        Integer limit
    );
}
