package com.sprint.otboo.feed.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface CommentApi {

    @Operation(summary = "피드 댓글 등록", description = "피드 댓글 등록 API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", description = "피드 댓글 등록 성공",
            content = @Content(schema = @Schema(implementation = CommentDto.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "피드 댓글 등록 실패",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<CommentDto> create(UUID feedId, CommentCreateRequest request);

    @Operation(
        summary = "피드 댓글 조회",
        description = "피드 댓글 조회 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "피드 댓글 조회 성공",
            content = @Content(schema = @Schema(implementation = CursorPageResponse.class),
                examples = @ExampleObject(value = """
                    {
                         "data": [
                             {
                                 "id": "de36164b-7f20-40c7-b5a8-2c877d01ec14",
                                 "createdAt": "2025-10-14T04:59:48.118339Z",
                                 "feedId": "25c986b6-7c2e-4396-864a-0ce8bedd8692",
                                 "author": {
                                     "userId": "11111111-1111-1111-1111-111111111111",
                                     "name": "name",
                                     "profileImageUrl": "https://img.example.com/user.png"
                                 },
                                 "content": "content"
                             }
                         ],
                         "nextCursor": null,
                         "nextIdAfter": null,
                         "hasNext": false,
                         "totalCount": 1,
                         "sortBy": "createdAt",
                         "sortDirection": "DESCENDING"
                    } 
                    """

                ))

        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 파라미터",
            content = @Content(schema = @Schema(hidden = true))
        ),
        @ApiResponse(
            responseCode = "404", description = "Feed를 찾을 수 없음",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<CursorPageResponse<CommentDto>> getComments(
        @Parameter(description = "feedId")
        @PathVariable UUID feedId,
        @Parameter(description = "cursor")
        @RequestParam(required = false) String cursor,
        @Parameter(description = "idAfter")
        @RequestParam(required = false) UUID idAfter,
        @Parameter(description = "limit")
        @RequestParam int limit
    );
}
