package com.sprint.otboo.fixture;

import com.sprint.otboo.weather.entity.Weather;
import java.util.UUID;

public class WeatherFixture {

    public static Weather create(UUID id) {
        return Weather.builder()
            .id(id)
            .build();
    }
}
