package com.sprint.otboo.weather.controller;

import com.sprint.otboo.common.exception.weather.WeatherBadCoordinateException;
import com.sprint.otboo.weather.dto.data.WeatherDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import com.sprint.otboo.weather.service.WeatherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weathers")
public class WeatherController implements WeatherApi {

    private final WeatherService weatherService;
    private final WeatherLocationQueryService locationQueryService;

    @Override
    @GetMapping("")
    public ResponseEntity<List<WeatherDto>> getWeathers(
        @RequestParam("longitude") double longitude,
        @RequestParam("latitude") double latitude
    ) {
        validateRange(latitude, longitude);
        return ResponseEntity.ok(weatherService.getWeather(latitude, longitude));
    }

    @Override
    @GetMapping("/location")
    public ResponseEntity<WeatherLocationResponse> getWeatherLocation(
        @RequestParam("longitude") double longitude,
        @RequestParam("latitude") double latitude
    ) {
        validateRange(latitude, longitude);
        return ResponseEntity.ok(locationQueryService.getWeatherLocation(latitude, longitude));
    }

    private static void validateRange(double lat, double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            WeatherBadCoordinateException ex = new WeatherBadCoordinateException();
            ex.addDetail("latitude", String.valueOf(lat));
            ex.addDetail("longitude", String.valueOf(lon));
            throw ex;
        }
    }
}
