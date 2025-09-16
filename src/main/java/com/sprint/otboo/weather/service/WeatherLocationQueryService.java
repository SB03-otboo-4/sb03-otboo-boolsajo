package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;

public interface WeatherLocationQueryService {

    WeatherLocationResponse getWeatherLocation(double longitude, double latitude);
}