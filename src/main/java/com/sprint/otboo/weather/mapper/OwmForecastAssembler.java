package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.owm.OwmInference;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import java.time.Instant;

public class OwmForecastAssembler {

    private final boolean percentProbability;
    private final WindStrengthResolver windStrengthResolver;

    public OwmForecastAssembler(boolean percentProbability, WindStrengthResolver windStrengthResolver) {
        this.percentProbability = percentProbability;
        this.windStrengthResolver = windStrengthResolver;
    }

    public Weather toEntity(WeatherLocation location, CollectedForecast c, Instant ingestedAt) {
        // 1) 추론 값 계산
        OwmInference.SkyStatus owmSky = OwmInference.toSkyStatus(c.cloudsAllPct(), c.weatherId());
        OwmInference.PrecipitationType owmType = OwmInference.toPrecipType(c.weatherId());

        // 2) 실수치 계산
        double amountMm = c.rain3hMm() != null ? c.rain3hMm()
            : (c.snow3hMm() != null ? c.snow3hMm() : 0d);
        double probability = OwmInference.toProbability(c.pop(), percentProbability);
        Double speedMs = c.windSpeedMs();

        // 3) enum 매핑 (이름 동일하다는 전제. 다르면 매핑 테이블 작성)
        SkyStatus skyStatus = SkyStatus.valueOf(owmSky.name());
        PrecipitationType type = PrecipitationType.valueOf(owmType.name());
        WindStrength asWord = windStrengthResolver.resolve(speedMs);

        // 4) 엔티티 생성 (대표치/전일 비교는 별도 배치에서 세팅)
        return Weather.builder()
            .location(location)
            .forecastedAt(ingestedAt)
            .forecastAt(c.forecastAt())
            .skyStatus(skyStatus)
            .asWord(asWord)
            .type(type)
            .speedMs(speedMs)
            .currentPct(c.humidityPct() == null ? 0d : c.humidityPct().doubleValue())
            .comparedPct(null)
            .currentC(round1(c.temperatureC()))
            .comparedC(null)
            .minC(null)
            .maxC(null)
            .amountMm(round1(amountMm))
            .probability(probability)
            .build();
    }

    private static double round1(Double v) {
        if (v == null) return 0d;
        return Math.round(v * 10d) / 10d;
    }
}
