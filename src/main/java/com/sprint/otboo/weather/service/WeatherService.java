package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import java.util.List;

public interface WeatherService {

    List<WeatherSummaryDto> getWeather(Double latitude, Double longitude);
}