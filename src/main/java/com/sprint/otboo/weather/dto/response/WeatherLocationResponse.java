package com.sprint.otboo.weather.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record WeatherLocationResponse(
    @Schema(description = "위도", example = "37.5665")
    double latitude,

    @Schema(description = "경도", example = "126.9780")
    double longitude,

    @Schema(description = "x", example = "60")
    int x,

    @Schema(description = "y", example = "127")
    int y,

    @Schema(description = "지명", example = "[\"서울특별시\",\"중구\",\"태평로1가\"]")
    List<String> locationNames
) {
    public WeatherLocationResponse {
        locationNames = (locationNames == null) ? List.of() : List.copyOf(locationNames);
    }
}