package com.sprint.otboo.weather.integration.spi;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public interface WeatherDataClient {

    List<CollectedForecast> fetch(double latitude, double longitude, Locale locale);

    record CollectedForecast(
        Instant forecastAt,
        double temperatureC,
        Integer humidityPct,
        Double windSpeedMs,
        Double pop,         // 0..1
        Double rain3hMm,
        Double snow3hMm,
        Integer cloudsAllPct,
        Integer weatherId
    ) {}
}
