package com.sprint.otboo.fixture;

import com.sprint.otboo.weather.entity.WeatherLocation;
import java.time.Instant;

public final class WeatherLocationFixture {

    private WeatherLocationFixture() {
    }

    public static final String DEFAULT_LOCATION_NAMES = "Seoul";
    public static final double DEFAULT_LATITUDE = 37.5665;
    public static final double DEFAULT_LONGITUDE = 126.9780;
    public static final Integer DEFAULT_X = 60;
    public static final Integer DEFAULT_Y = 127;

    public static WeatherLocation createLocationWithDefault() {
        return WeatherLocation.builder()
            .locationNames(DEFAULT_LOCATION_NAMES)
            .latitude(DEFAULT_LATITUDE)
            .longitude(DEFAULT_LONGITUDE)
            .x(DEFAULT_X)
            .y(DEFAULT_Y)
            .createdAt(Instant.now())
            .build();
    }

    public static WeatherLocation create(String locationNames, double lat, double lon, int x,
        int y) {
        return WeatherLocation.builder()
            .locationNames(locationNames)
            .latitude(lat)
            .longitude(lon)
            .x(x)
            .y(y)
            .createdAt(Instant.now())
            .build();
    }
}
