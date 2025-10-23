package com.sprint.otboo.weather.integration.owm.mapper;

import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OWM 3시간 예보를 날짜별 일 최고/최저 온도로 집계한다.
 */
public final class OwmForecastDailyAggregator {

    private OwmForecastDailyAggregator() {}

    public static Map<LocalDate, DailyTemperature> aggregate(List<CollectedForecast> forecasts, ZoneId zone) {
        return forecasts.stream()
            .collect(Collectors.groupingBy(
                f -> LocalDate.ofInstant(f.forecastAt(), zone),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> {
                        double min = list.stream().mapToDouble(CollectedForecast::temperatureC).min().orElse(0);
                        double max = list.stream().mapToDouble(CollectedForecast::temperatureC).max().orElse(0);
                        return new DailyTemperature(min, max);
                    }
                )
            ));
    }

    public record DailyTemperature(double minC, double maxC) {}
}
