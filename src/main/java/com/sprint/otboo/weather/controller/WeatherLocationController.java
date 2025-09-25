package com.sprint.otboo.weather.controller;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weathers")
public class WeatherLocationController {

    private final WeatherLocationQueryService service;

    @GetMapping("/location")
    public WeatherLocationResponse getWeatherLocation(
        @RequestParam("longitude")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0",  message = "경도는 180 이하이어야 합니다.")
        double longitude,

        @RequestParam("latitude")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0",  message = "위도는 90 이하이어야 합니다.")
        double latitude

    ) {
        // 서비스는 (lat, lon) 순서
        return service.getWeatherLocation(latitude, longitude);
    }
}
