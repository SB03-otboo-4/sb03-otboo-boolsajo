package com.sprint.otboo.weather.integration.owm.mapper;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.integration.owm.OwmInference;
import com.sprint.otboo.weather.integration.owm.mapper.OwmForecastDailyAggregator.DailyTemperature;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

public class OwmForecastAssembler {

    private final boolean percentProbability;
    private final WindStrengthResolver windStrengthResolver;
    private final Map<LocalDate, DailyTemperature> dailyTempMap;
    private final ZoneId zone;

    public OwmForecastAssembler(
        boolean percentProbability,
        WindStrengthResolver resolver,
        Map<LocalDate, DailyTemperature> dailyTempMap,
        ZoneId zone
    ) {
        this.percentProbability = percentProbability;
        this.windStrengthResolver = resolver;
        this.dailyTempMap = dailyTempMap;
        this.zone = zone;
    }

    public Weather toEntity(WeatherLocation location, CollectedForecast c, Instant forecastedAt) {
        OwmInference.SkyStatus owmSky = OwmInference.toSkyStatus(c.cloudsAllPct(), c.weatherId());
        OwmInference.PrecipitationType owmType = OwmInference.toPrecipType(c.weatherId());

        SkyStatus skyStatus = SkyStatus.valueOf(owmSky.name());
        PrecipitationType type = PrecipitationType.valueOf(owmType.name());
        double speedMs = safe(c.windSpeedMs());
        WindStrength asWord = windStrengthResolver.resolve(speedMs);
        double amountMm = safe(c.rain3hMm()) + safe(c.snow3hMm());
        double probability = OwmInference.toProbabilityNormalized(c.pop(), this.percentProbability);
        double currentC = round1(safe(c.temperatureC()));

        LocalDate date = LocalDate.ofInstant(c.forecastAt(), zone);
        DailyTemperature dayTemp = dailyTempMap.getOrDefault(date, new DailyTemperature(currentC, currentC));

        return Weather.builder()
            .location(location)
            .forecastedAt(forecastedAt)
            .forecastAt(c.forecastAt())
            .skyStatus(skyStatus)
            .asWord(asWord)
            .type(type)
            .speedMs(round1(speedMs))
            .currentPct(c.humidityPct() == null ? 0d : c.humidityPct().doubleValue())
            .currentC(currentC)
            .minC(round1(dayTemp.minC()))
            .maxC(round1(dayTemp.maxC()))
            .amountMm(round1(amountMm))
            .probability(probability)
            .build();
    }

    private static double safe(Double v) { return v == null ? 0d : v; }
    private static double round1(double v) { return Math.round(v * 10d) / 10d; }
}
