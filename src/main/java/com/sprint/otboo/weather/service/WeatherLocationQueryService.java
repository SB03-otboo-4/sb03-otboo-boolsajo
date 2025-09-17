package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;

public interface WeatherLocationQueryService {

    // (latitude, longitude) 순서로 호출
    WeatherLocationResponse getWeatherLocation(double latitude, double longitude);
}