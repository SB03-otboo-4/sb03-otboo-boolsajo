package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import org.springframework.stereotype.Service;

@Service
public class WeatherLocationQueryServiceImpl implements WeatherLocationQueryService {

    @Override
    public WeatherLocationResponse getWeatherLocation(double longitude, double latitude) {
        // GREEN 단계에서 실제 구현
        throw new UnsupportedOperationException("TBD - GREEN 단계에서 구현");
    }
}