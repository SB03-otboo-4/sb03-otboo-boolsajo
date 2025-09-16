package com.sprint.otboo.weather.controller;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherLocationQueryService service;

    @GetMapping("/location")
    public WeatherLocationResponse getWeatherLocation(
        @RequestParam("longitude") double longitude,
        @RequestParam("latitude") double latitude
    ) {
        return service.getWeatherLocation(longitude, latitude);
    }
}
