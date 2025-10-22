package com.sprint.otboo.recommendation.controller.api;

import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "추천 관리", description = "추천 관련 API")
@RequestMapping("/api/recommendations")
public interface RecommendationApi {

    @Operation(summary = "추천 조회", description = "특정 사용자와 날씨 정보를 기반으로 추천 의상을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추천 조회 성공",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = RecommendationDto.class),
                examples = @ExampleObject(value = """
                {
                  "weatherId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "clothes": [
                    {
                      "clothesId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "name": "셔츠",
                      "imageUrl": "http://example.com/image.png",
                      "type": "TOP",
                      "attributes": [
                        {
                          "definitionId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                          "definitionName": "색상",
                          "selectableValues": ["빨강", "파랑"],
                          "value": "빨강"
                        }
                      ]
                    }
                  ]
                }
                """)
            )
        ),
        @ApiResponse(responseCode = "400", description = "추천 조회 실패",
            content = @Content(mediaType = "*/*", schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<RecommendationDto> getRecommendations(
        @Parameter(description = "날씨 정보 ID", required = true) @RequestParam UUID weatherId
    );
}