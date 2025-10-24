package com.sprint.otboo.weather.integration.owm;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.integration.owm.dto.OwmForecastResponse;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import com.sprint.otboo.weather.integration.owm.mapper.OwmForecastMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OwmForecastMapperTest {

    @Test
    void 정상_아이템_하나를_CollectedForecast로_변환한다() {
        OwmForecastResponse.Item item = new OwmForecastResponse.Item(
            1_700_000_000L,
            "2023-10-10 12:00:00",
            new OwmForecastResponse.Main(12.3, 12.0, 12.0, 13.0, 1013, 55),
            new OwmForecastResponse.Wind(3.1, 270, 5.0),
            new OwmForecastResponse.Clouds(88),
            new OwmForecastResponse.Rain(1.2),
            null,
            0.7,
            List.of(new OwmForecastResponse.Weather(500, "Rain", "light rain", "10n")),
            new OwmForecastResponse.Sys("d")
        );

        OwmForecastResponse res = new OwmForecastResponse(
            List.of(item),
            new OwmForecastResponse.City(32400)
        );

        List<CollectedForecast> out = OwmForecastMapper.toCollected(res);
        assertThat(out).hasSize(1);

        CollectedForecast c = out.get(0);
        assertThat(c.forecastAt()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
        assertThat(c.temperatureC()).isEqualTo(12.3);
        assertThat(c.humidityPct()).isEqualTo(55);
        assertThat(c.windSpeedMs()).isEqualTo(3.1);
        assertThat(c.pop()).isEqualTo(0.7);
        assertThat(c.rain3hMm()).isEqualTo(1.2);
        assertThat(c.snow3hMm()).isEqualTo(0.0);
        assertThat(c.cloudsAllPct()).isEqualTo(88);
        assertThat(c.weatherId()).isEqualTo(500);
    }

    @Test
    void weather_배열이_null_또는_비어있으면_weatherId는_null이다() {
        OwmForecastResponse.Item item1 = new OwmForecastResponse.Item(
            1_700_000_001L,
            "2023-10-10 15:00:00",
            new OwmForecastResponse.Main(10.0, 10.0, 9.0, 11.0, 1000, 60),
            new OwmForecastResponse.Wind(2.0, 200, 3.0),
            new OwmForecastResponse.Clouds(10),
            null,
            null,
            0.1,
            null,
            new OwmForecastResponse.Sys("d")
        );
        OwmForecastResponse.Item item2 = new OwmForecastResponse.Item(
            1_700_000_002L,
            "2023-10-10 18:00:00",
            new OwmForecastResponse.Main(11.0, 10.0, 9.0, 12.0, 1000, 61),
            new OwmForecastResponse.Wind(3.0, 180, 4.0),
            new OwmForecastResponse.Clouds(20),
            null,
            null,
            0.2,
            List.of(),
            new OwmForecastResponse.Sys("n")
        );

        OwmForecastResponse res = new OwmForecastResponse(
            List.of(item1, item2),
            new OwmForecastResponse.City(32400)
        );

        List<CollectedForecast> out = OwmForecastMapper.toCollected(res);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).weatherId()).isNull();
        assertThat(out.get(1).weatherId()).isNull();
    }

    @Test
    void 내부필드가_null이면_모두_0으로_보정된다() {
        OwmForecastResponse.Item item = new OwmForecastResponse.Item(
            1_700_000_003L,
            "2023-10-10 21:00:00",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        OwmForecastResponse res = new OwmForecastResponse(
            List.of(item),
            new OwmForecastResponse.City(32400)
        );

        List<CollectedForecast> out = OwmForecastMapper.toCollected(res);
        assertThat(out).hasSize(1);
        CollectedForecast c = out.get(0);

        assertThat(c.temperatureC()).isEqualTo(0.0);
        assertThat(c.humidityPct()).isEqualTo(0);
        assertThat(c.windSpeedMs()).isEqualTo(0.0);
        assertThat(c.rain3hMm()).isEqualTo(0.0);
        assertThat(c.snow3hMm()).isEqualTo(0.0);
        assertThat(c.cloudsAllPct()).isEqualTo(0);
        assertThat(c.weatherId()).isNull();
        assertThat(c.pop()).isNull();
    }

    @Test
    void 응답이_null이거나_list가_null이면_빈리스트를_반환한다() {
        assertThat(OwmForecastMapper.toCollected(null)).isEmpty();

        OwmForecastResponse resWithNullList = new OwmForecastResponse(
            null,
            new OwmForecastResponse.City(32400)
        );
        assertThat(OwmForecastMapper.toCollected(resWithNullList)).isEmpty();
    }

    @Test
    void 여러_아이템이_순서대로_변환된다() {
        OwmForecastResponse.Item a = new OwmForecastResponse.Item(
            10L,
            "2023-10-11 00:00:00",
            new OwmForecastResponse.Main(1.0, 1.0, 0.0, 2.0, 1000, 50),
            new OwmForecastResponse.Wind(1.0, 0, 0.0),
            new OwmForecastResponse.Clouds(1),
            null,
            null,
            0.1,
            List.of(new OwmForecastResponse.Weather(100, "", "", "")),
            null
        );
        OwmForecastResponse.Item b = new OwmForecastResponse.Item(
            20L,
            "2023-10-11 03:00:00",
            new OwmForecastResponse.Main(2.0, 2.0, 1.0, 3.0, 1000, 60),
            new OwmForecastResponse.Wind(2.0, 0, 0.0),
            new OwmForecastResponse.Clouds(2),
            null,
            null,
            0.2,
            List.of(new OwmForecastResponse.Weather(200, "", "", "")),
            null
        );

        OwmForecastResponse res = new OwmForecastResponse(
            List.of(a, b),
            new OwmForecastResponse.City(32400)
        );

        List<CollectedForecast> out = OwmForecastMapper.toCollected(res);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).forecastAt()).isEqualTo(Instant.ofEpochSecond(10L));
        assertThat(out.get(0).temperatureC()).isEqualTo(1.0);
        assertThat(out.get(0).weatherId()).isEqualTo(100);

        assertThat(out.get(1).forecastAt()).isEqualTo(Instant.ofEpochSecond(20L));
        assertThat(out.get(1).temperatureC()).isEqualTo(2.0);
        assertThat(out.get(1).weatherId()).isEqualTo(200);
    }
}
