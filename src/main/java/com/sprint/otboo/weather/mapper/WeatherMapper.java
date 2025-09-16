package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.util.List;

public class WeatherMapper {

    private WeatherMapper() {}
    public static WeatherLocationResponse toLocationResponse(WeatherLocation wl) {
        return new WeatherLocationResponse(
            wl.getLatitude(),
            wl.getLongitude(),
            wl.getX(),
            wl.getY(),
            List.of()
        );
    }
}
