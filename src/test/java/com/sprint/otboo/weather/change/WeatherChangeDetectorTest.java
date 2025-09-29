package com.sprint.otboo.weather.change;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("특이 기상 변화 디텍터 테스트")
class WeatherChangeDetectorTest {

    @Test
    void 강수형태가_변경되면_변화로_감지한다() {

        WeatherLocation loc = WeatherLocation.builder().build();
        loc.setId(UUID.randomUUID());

        Weather older = Weather.builder()
            .location(loc)
            .forecastAt(Instant.parse("2025-09-28T00:00:00Z"))
            .forecastedAt(Instant.parse("2025-09-27T23:00:00Z"))
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .currentC(22.0)
            .probability(10.0)
            .build();

        Weather newer = Weather.builder()
            .location(loc)
            .forecastAt(older.getForecastAt())
            .forecastedAt(Instant.parse("2025-09-28T02:00:00Z"))
            .skyStatus(SkyStatus.CLOUDY)
            .type(PreciptationType.RAIN)
            .currentC(21.0)
            .probability(60.0)
            .build();

        WeatherChangeThresholdProperties th = new WeatherChangeThresholdProperties();
        th.setTempJumpC(5.0);
        th.setPopJumpPct(30.0);

        WeatherChangeDetector detector = new WeatherChangeDetector();

        List<DetectedChange> changes = detector.detect(older, newer, th);

        assertThat(changes.stream().anyMatch(c -> c.type() == WeatherChangeType.PTY_CHANGE)).isTrue();
    }

    @Test
    void 온도_변화가_임계값_미만이면_감지하지_않는다() {

        WeatherLocation loc = WeatherLocation.builder().build();
        loc.setId(UUID.randomUUID());

        Weather older = Weather.builder()
            .location(loc)
            .forecastAt(Instant.parse("2025-09-28T03:00:00Z"))
            .forecastedAt(Instant.parse("2025-09-28T02:00:00Z"))
            .skyStatus(SkyStatus.CLEAR)
            .type(PreciptationType.NONE)
            .currentC(20.0)
            .probability(0.0)
            .build();

        Weather newer = Weather.builder()
            .location(loc)
            .forecastAt(older.getForecastAt())
            .forecastedAt(Instant.parse("2025-09-28T05:00:00Z"))
            .skyStatus(SkyStatus.CLEAR)
            .type(PreciptationType.NONE)
            .currentC(24.9) // +4.9℃
            .probability(0.0)
            .build();

        WeatherChangeThresholdProperties th = new WeatherChangeThresholdProperties();
        th.setTempJumpC(5.0);
        th.setPopJumpPct(30.0);

        WeatherChangeDetector detector = new WeatherChangeDetector();

        List<DetectedChange> changes = detector.detect(older, newer, th);

        assertThat(changes.stream().noneMatch(c -> c.type() == WeatherChangeType.TEMP_JUMP)).isTrue();
    }
}
