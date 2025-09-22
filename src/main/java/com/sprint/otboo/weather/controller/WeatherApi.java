package com.sprint.otboo.weather.controller;

import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "날씨 관리", description = "날씨 관련 API")
@RequestMapping("/api/weathers")
public interface WeatherApi {

    @Operation(
        summary = "날씨 위치 정보 조회",
        description = "위·경도를 받아 기상청 단기예보용 격자(X,Y)와 지명 배열을 반환합니다."
    )
    @ApiResponse(
        responseCode = "200",
        description = "날씨 위치 정보 조회 성공",
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
            examples = @ExampleObject(name = "좌표 오류", value = """
            {
              "exceptionName": "WeatherBadCoordinateException",
              "message": "WEATHER_BAD_COORDINATE 잘못된 좌표 값입니다.",
              "details": { "latitude":"-100.0", "longitude":"200.0" }
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
