package com.sprint.otboo.feed.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface FeedApi {

    @Operation(summary = "피드 등록", description = "피드 등록 API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", description = "피드 등록 성공",
            content = @Content(schema = @Schema(implementation = FeedDto.class),
                examples = @ExampleObject(value = """
                    {
                      "id": "74126c0a-cc39-4b3e-8847-d5ab44c46c8d",
                      "createdAt": "2025-10-15T02:58:00Z",
                      "updatedAt": "2025-10-15T02:58:00Z",
                      "author": {
                        "userId": "5292a576-80a2-4e40-9f82-7a572fb2acd8",
                        "name": "You",
                        "profileImageUrl": "https://img.example.com/me.png"
                      },
                      "weather": {
                        "weatherId": "e4e23f0b-d65a-43ba-8663-7bf1b03e8888",
                        "skyStatus": "CLOUDY",
                        "precipitation": { "type": "NONE", "amount": 0.0, "probability": 30.0 },
                        "temperature": { "current": 21.0, "min": 18.0, "max": 24.0 }
                      },
                      "ootds": [
                        {
                          "clothesId": "f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1",
                          "name": "블루 셔츠",
                          "imageUrl": "https://img.example.com/shirt1.png",
                          "type": "TOP"
                        },
                        {
                          "clothesId": "f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2",
                          "name": "블랙 팬츠",
                          "imageUrl": "https://img.example.com/pants1.png",
                          "type": "BOTTOM"
                        }
                      ],
                      "content": "오늘 코디 공유",
                      "likeCount": 0,
                      "commentCount": 0,
                      "likedByMe": false
                    }
                    
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400", description = "피드 등록 실패",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<FeedDto> create(FeedCreateRequest request);

    @Operation(
        summary = "피드 목록 조회",
        description = "피드 목록 조회 API"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "피드 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = CursorPageResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "data": [
                        {
                          "id": "74126c0a-cc39-4b3e-8847-d5ab44c46c8d",
                          "createdAt": "2025-10-14T07:25:10Z",
                          "author": { "userId": "22222222-2222-2222-2222-222222222222", "name": "name" },
                          "content": "content",
                          "likeCount": 3,
                          "commentCount": 7,
                          "likedByMe": false
                        },
                      ],
                      "nextCursor": "1760426110118",
                      "hasNext": true,
                      "totalCount": 46,
                      "sortBy": "createdAt",
                      "sortDirection": "DESCENDING"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", description = "피드 목록 조회 실패",
            content = @Content(schema = @Schema(hidden = true))
        )
    })
    ResponseEntity<CursorPageResponse<FeedDto>> getFeeds(
        @Parameter(description = "cursor")
        @RequestParam(required = false) String cursor,

        @Parameter(description = "idAfter")
        @RequestParam(required = false) UUID idAfter,

        @Parameter(description = "limit")
        @RequestParam int limit,

        @Parameter(in = ParameterIn.QUERY,
            schema = @Schema(type = "string", allowableValues = {"createdAt",
                "likeCount"})
        )
        @RequestParam String sortBy,

        @Parameter(in = ParameterIn.QUERY,
            schema = @Schema(type = "string", allowableValues = {"ASCENDING",
                "DESCENDING"})
        )
        @RequestParam String sortDirection,

        @Parameter(description = "keywordLike")
        @RequestParam(required = false) String keywordLike,

        @Parameter(in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {
            "CLEAR", "MOSTLY_CLOUDY", "CLOUDY"}))
        @RequestParam(name = "skyStatusEqual", required = false) SkyStatus skyStatusEqual,

        @Parameter(in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {
            "NONE", "RAIN", "RAIN_SNOW", "SNOW", "SHOWER"}))
        @RequestParam(name = "precipitationTypeEqual", required = false)
        PrecipitationType precipitationTypeEqual,

        @Parameter(description = "authorIdEqual")
        @RequestParam(name = "authorIdEqual", required = false)
        UUID authorIdEqual
    );

    @Operation(summary = " 피드 수정", description = "피드 수정 API")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", description = "피드 수정 성공",
            content = @Content(schema = @Schema(implementation = FeedDto.class))
        ),
        @ApiResponse(
            responseCode = "400", description = "잘못된 요청 본문",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401", description = "인증되지 않음",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403", description = "수정 권한 없음(작성자 불일치)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404", description = "작성자 또는 Feed를 찾을 수 없음",
            content = @Content
        )
    })
    ResponseEntity<FeedDto> update(
        @Parameter(description = "feedId", required = true)
        UUID feedId,
        @Parameter(hidden = true)
        CustomUserDetails user,
        @RequestBody(
            required = true,
            description = "RequestBody",
            content = @Content(schema = @Schema(implementation = FeedUpdateRequest.class))
        )
        @Valid FeedUpdateRequest request
    );

    @Operation(summary = "피드 삭제", description = "피드 삭제 API")
    @ApiResponses({
        @ApiResponse(
            responseCode = "204", description = "피드 삭제 성공"
        ),
        @ApiResponse(
            responseCode = "401", description = "인증되지 않음"
        ),
        @ApiResponse(
            responseCode = "403", description = "삭제 권한 없음(작성자 불일치)"
        ),
        @ApiResponse(
            responseCode = "404", description = "작성자 또는 Feed를 찾을 수 없음"
        )
    })
    ResponseEntity<Void> delete(
        @Parameter(description = "feedId", required = true) UUID feedId,
        @Parameter(hidden = true) CustomUserDetails user
    );
}
