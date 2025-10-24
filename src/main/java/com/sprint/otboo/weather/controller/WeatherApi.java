package com.sprint.otboo.weather.controller;

import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.weather.dto.data.WeatherDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "날씨 관리", description = "날씨 관련 API")
@RequestMapping("/api/weathers")
public interface WeatherApi {

    @Operation(
        summary = "날씨 요약 조회",
        description = "위·경도를 받아 해당 위치의 단기예보 스냅샷을 요약 리스트로 반환합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = WeatherSummaryDto.class)),
            examples = @ExampleObject(name = "성공 예시", value = """
            [
              {
                "weatherId": "d5f9bd2a-2c3e-4ec5-8b50-2e9cf2c2a1b9",
                "skyStatus": "CLEAR",
                "precipitation": { "type": "NONE", "amountMm": 0.0, "probability": 10.0 },
                "temperature": { "current": 22.0, "compared": 0.0, "min": 0.0, "max": 0.0 }
              }
            ]
            """)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "요청 값 오류(누락/형식/좌표 범위 등)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = {
                @ExampleObject(name = "좌표 범위 오류", value = """
                {
                  "timestamp": "2025-09-28T12:34:56.789Z",
                  "code": "WEATHER_BAD_COORDINATE",
                  "message": "잘못된 좌표 값입니다.",
                  "details": { "latitude": "-100.0", "longitude": "200.0" },
                  "exceptionName": "WeatherBadCoordinateException",
                  "status": 400
                }
                """),
                @ExampleObject(name = "파라미터 누락", value = """
                {
                  "timestamp": "2025-09-28T12:34:56.789Z",
                  "code": "VALIDATION_FAILED",
                  "message": "요청 값 검증에 실패했습니다.",
                  "details": { "missing": "latitude" },
                  "exceptionName": "MissingServletRequestParameterException",
                  "status": 400
                }
                """)
            }
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "서버 오류", value = """
            {
              "timestamp": "2025-09-28T12:34:56.789Z",
              "code": "INTERNAL_SERVER_ERROR",
              "message": "서버 내부 오류가 발생했습니다.",
              "details": null,
              "exceptionName": "RuntimeException",
              "status": 500
            }
            """)
        )
    )
    @GetMapping("")
    ResponseEntity<List<WeatherDto>> getWeathers(
        @Parameter(description = "경도(동경,+), 범위: -180 ~ 180", example = "126.9780", required = true)
        @RequestParam("longitude") double longitude,
        @Parameter(description = "위도(북위,+), 범위: -90 ~ 90", example = "37.5665", required = true)
        @RequestParam("latitude") double latitude
    );

    @Operation(
        summary = "날씨 위치 정보 조회",
        description = "위·경도를 받아 기상청 단기예보용 격자(X,Y)와 지명 배열을 반환합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = WeatherLocationResponse.class),
            examples = @ExampleObject(name = "성공 예시", value = """
            {
              "latitude": 37.5665,
              "longitude": 126.9780,
              "x": 60,
              "y": 127,
              "locationNames": ["서울특별시","중구","태평로1가"]
            }
            """)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "요청 값 오류(좌표 범위 등)",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "좌표 범위 오류", value = """
            {
              "timestamp": "2025-09-28T12:34:56.789Z",
              "code": "WEATHER_BAD_COORDINATE",
              "message": "잘못된 좌표 값입니다.",
              "details": { "latitude": "-100.0", "longitude": "200.0" },
              "exceptionName": "WeatherBadCoordinateException",
              "status": 400
            }
            """)
        )
    )
    @GetMapping("/location")
    ResponseEntity<WeatherLocationResponse> getWeatherLocation(
        @Parameter(description = "경도(동경,+), 범위: -180 ~ 180", example = "126.9780", required = true)
        @RequestParam("longitude") double longitude,
        @Parameter(description = "위도(북위,+), 범위: -90 ~ 90", example = "37.5665", required = true)
        @RequestParam("latitude") double latitude
    );
}
