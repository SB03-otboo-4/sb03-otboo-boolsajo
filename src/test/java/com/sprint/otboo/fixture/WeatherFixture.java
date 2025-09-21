package com.sprint.otboo.fixture;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.entity.WindStrength;
import java.time.Instant;
import java.util.UUID;

public class WeatherFixture {

    public static final SkyStatus DEFAULT_SKY_STATUS = SkyStatus.CLEAR;
    public static final PrecipitationType DEFAULT_TYPE = PrecipitationType.NONE;
    public static final WindStrength DEFAULT_AS_WORD = WindStrength.WEAK;
    public static final double DEFAULT_CURRENT_C = 23.0;
    public static final double DEFAULT_PROB = 0.0;

    public static Weather createWeatherWithDefault(WeatherLocation location) {
        Instant now = Instant.now();
        return Weather.builder()
            .location(location)
            .forecastAt(now)
            .forecastedAt(now)
            .skyStatus(DEFAULT_SKY_STATUS)
            .asWord(DEFAULT_AS_WORD)
            .type(DEFAULT_TYPE)
            .currentC(DEFAULT_CURRENT_C)
            .probability(DEFAULT_PROB)
            .createdAt(now)
            .build();
    }

    public static Weather create(UUID id) {
        return Weather.builder()
            .id(id)
            .build();
    }

    public static Weather create(UUID id, SkyStatus skyStatus, PrecipitationType precipType) {
        return Weather.builder()
            .id(id)
            .skyStatus(skyStatus)
            .type(precipType)
            .build();
    }

    public static Weather create(SkyStatus skyStatus, PrecipitationType precipType) {
        return Weather.builder()
            .skyStatus(skyStatus)
            .type(precipType)
            .build();
    }
}
