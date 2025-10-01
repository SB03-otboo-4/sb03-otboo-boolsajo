package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.data.WeatherDto;
import java.util.List;

public interface WeatherService {

    List<WeatherDto> getWeather(Double latitude, Double longitude);
}