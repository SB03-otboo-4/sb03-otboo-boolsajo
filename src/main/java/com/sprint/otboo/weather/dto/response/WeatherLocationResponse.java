package com.sprint.otboo.weather.dto.response;

import java.util.List;

public record WeatherLocationResponse(
    double latitude,
    double longitude,
    int x,
    int y,
    List<String> locationNames
) { }