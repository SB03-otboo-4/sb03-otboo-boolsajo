package com.sprint.otboo.weather.integration.owm;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import com.sprint.otboo.weather.mapper.OwmForecastMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OWM 매퍼 테스트")
class OwmForecastMapperTest {

    @Test
    void OWM_응답을_CollectedForecast로_변환() {
        OwmForecastResponse.Item item = new OwmForecastResponse.Item(
            1_700_000_000L,
            new OwmForecastResponse.Main(12.3, 12.0, 12.6, 55, 1013.0),
            List.of(new OwmForecastResponse.Weather(500, "Rain", "light rain", "10n")),
            new OwmForecastResponse.Clouds(88),
            new OwmForecastResponse.Wind(3.1, 270, 5.0),
            0.7,
            new OwmForecastResponse.Rain(1.2),
            null,
            "2023-10-10 12:00:00"
        );
        OwmForecastResponse res = new OwmForecastResponse(new OwmForecastResponse.City(
            0, "X", new OwmForecastResponse.Coord(37.5, 127.0), "KR", 0, 0, 0
        ), List.of(item));

        List<CollectedForecast> out = OwmForecastMapper.toCollected(res);
        assertThat(out).hasSize(1);

        CollectedForecast c = out.get(0);
        assertThat(c.forecastAt()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
        assertThat(c.temperatureC()).isEqualTo(12.3);
        assertThat(c.humidityPct()).isEqualTo(55);
        assertThat(c.windSpeedMs()).isEqualTo(3.1);
        assertThat(c.pop()).isEqualTo(0.7);
        assertThat(c.rain3hMm()).isEqualTo(1.2);
        assertThat(c.snow3hMm()).isNull();
        assertThat(c.cloudsAllPct()).isEqualTo(88);
        assertThat(c.weatherId()).isEqualTo(500);
    }
}
