package com.sprint.otboo.weather.controller;

import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.service.WeatherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/api/weathers")
    public List<WeatherSummaryDto> getWeathers(
        @RequestParam double latitude,
        @RequestParam double longitude
    ) {
        return weatherService.getWeather(latitude, longitude);
    }
}
