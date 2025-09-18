package com.sprint.otboo.fixture;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.UUID;

public class WeatherFixture {

    public static Weather create(UUID id) {
        return Weather.builder()
            .id(id)
            .build();
    }

    public static Weather createWeather() {
        return Weather.builder()
            .id(UUID.randomUUID())
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .comparedC(23.0)
            .createdAt(Instant.now())
            .build();
    }

    public static Weather create(UUID id, SkyStatus skyStatus, PrecipitationType precipType) {
        return Weather.builder()
            .id(id)
            .skyStatus(skyStatus)
            .type(precipType)
            .build();
    }
}
