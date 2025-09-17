package com.sprint.otboo.weather.dto.response;

import java.util.List;

public record WeatherLocationResponse(
    double latitude,
    double longitude,
    int x,
    int y,
    List<String> locationNames
) {
    public WeatherLocationResponse {
        locationNames = (locationNames == null) ? List.of() : List.copyOf(locationNames);
    }
}