package com.sprint.otboo.weather.controller;

import com.sprint.otboo.common.exception.weather.WeatherBadCoordinateException;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import com.sprint.otboo.weather.service.WeatherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WeatherController implements WeatherApi {

    private final WeatherService weatherService;
    private final WeatherLocationQueryService locationQueryService;

    @Override
    public ResponseEntity<List<WeatherSummaryDto>> getWeathers(double longitude, double latitude) {

        validateRange(latitude, longitude);
        return ResponseEntity.ok(weatherService.getWeather(latitude, longitude));
    }

    @Override
    public ResponseEntity<WeatherLocationResponse> getWeatherLocation(double longitude, double latitude) {

        validateRange(latitude, longitude);
        WeatherLocationResponse body = locationQueryService.getWeatherLocation(latitude, longitude);
        return ResponseEntity.ok(body);
    }

    private static void validateRange(double latitude, double longitude) {

        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            WeatherBadCoordinateException ex =
                new WeatherBadCoordinateException();
            ex.addDetail("latitude", String.valueOf(latitude));
            ex.addDetail("longitude", String.valueOf(longitude));
            throw ex;
        }
    }
}
