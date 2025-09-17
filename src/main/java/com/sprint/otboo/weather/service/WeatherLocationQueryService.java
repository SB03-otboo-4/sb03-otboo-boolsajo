package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;

public interface WeatherLocationQueryService {

    // (longitude, latitude) 순서로 호출
    WeatherLocationResponse getWeatherLocation(double longitude, double latitude);
}