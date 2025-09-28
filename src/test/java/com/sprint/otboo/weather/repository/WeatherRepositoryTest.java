package com.sprint.otboo.weather.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("WeatherRepository 테스트")
class WeatherRepositoryTest {

    @TestConfiguration
    static class QuerydslTestConfig {
        @PersistenceContext
        private EntityManager em;

        @Bean
        JPAQueryFactory jpaQueryFactory() {
            return new JPAQueryFactory(em);
        }
    }

    @Autowired private WeatherRepository weatherRepository;
    @Autowired private WeatherLocationRepository locationRepository;

    // ===== Helpers =====
    private WeatherLocation saveLocation(double lat, double lon, int x, int y, String names) {
        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setLatitude(BigDecimal.valueOf(lat));
        location.setLongitude(BigDecimal.valueOf(lon));
        location.setX(x);
        location.setY(y);
        location.setLocationNames(names);
        return locationRepository.saveAndFlush(location);
    }

    private Weather saveWeather(
        WeatherLocation location,
        Instant forecastAt,
        Instant forecastedAt
    ) {
        Weather weather = Weather.builder()
            .location(location)
            .forecastAt(forecastAt)
            .forecastedAt(forecastedAt)
            .skyStatus(SkyStatus.CLEAR)
            .asWord(WindStrength.MODERATE)
            .type(PrecipitationType.NONE)
            .currentC(20.0)
            .probability(0.0)
            .build();
        return weatherRepository.saveAndFlush(weather);
    }

    // ===== Tests =====

    @Test
    void 유니크_제약_위반_발생해야_한다() {
        WeatherLocation location = saveLocation(37.5665, 126.9780, 60, 127, "서울특별시 중구 태평로1가");
        Instant forecastAt = Instant.parse("2025-09-25T03:00:00Z");
        Instant forecastedAt = Instant.parse("2025-09-25T02:30:00Z");

        saveWeather(location, forecastAt, forecastedAt);

        assertThrows(DataIntegrityViolationException.class, () ->
            saveWeather(location, forecastAt, forecastedAt)
        );
    }

    @Test
    void 동일_forecastAt에서_최신_2건을_반환한다() {
        WeatherLocation location = saveLocation(37.5665, 126.9780, 60, 127, "서울특별시 중구 태평로1가");
        Instant forecastAt = Instant.parse("2025-09-25T06:00:00Z");

        saveWeather(location, forecastAt, Instant.parse("2025-09-25T04:10:00Z"));
        Weather second = saveWeather(location, forecastAt, Instant.parse("2025-09-25T04:40:00Z"));
        Weather latest = saveWeather(location, forecastAt, Instant.parse("2025-09-25T05:10:00Z"));

        List<Weather> top2 = weatherRepository
            .findTop2ByLocationIdAndForecastAtOrderByForecastedAtDesc(location.getId(), forecastAt);

        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).getForecastedAt()).isEqualTo(latest.getForecastedAt());
        assertThat(top2.get(1).getForecastedAt()).isEqualTo(second.getForecastedAt());
    }

    @Test
    void 기간_조회시_forecastAt_오름차순_forecastedAt_내림차순으로_정렬된다() {
        WeatherLocation location = saveLocation(35.1796, 129.0756, 98, 76, "부산광역시 중구 중앙동");

        Instant at1 = Instant.parse("2025-09-25T00:00:00Z");
        Instant at2 = Instant.parse("2025-09-25T03:00:00Z");
        Instant at3 = Instant.parse("2025-09-25T06:00:00Z");

        saveWeather(location, at1, Instant.parse("2025-09-24T23:10:00Z"));
        Weather at1Latest = saveWeather(location, at1, Instant.parse("2025-09-24T23:40:00Z"));

        saveWeather(location, at2, Instant.parse("2025-09-25T02:10:00Z"));
        Weather at2Latest = saveWeather(location, at2, Instant.parse("2025-09-25T02:40:00Z"));

        saveWeather(location, at3, Instant.parse("2025-09-25T05:10:00Z"));
        Weather at3Latest = saveWeather(location, at3, Instant.parse("2025-09-25T05:40:00Z"));

        List<Weather> list = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                location.getId(),
                Instant.parse("2025-09-24T23:00:00Z"),
                Instant.parse("2025-09-25T07:00:00Z")
            );

        assertThat(list).hasSize(6);

        assertThat(list.get(0).getForecastAt()).isEqualTo(at1);
        assertThat(list.get(1).getForecastAt()).isEqualTo(at1);
        assertThat(list.get(0).getForecastedAt()).isEqualTo(at1Latest.getForecastedAt());

        assertThat(list.get(2).getForecastAt()).isEqualTo(at2);
        assertThat(list.get(3).getForecastAt()).isEqualTo(at2);
        assertThat(list.get(2).getForecastedAt()).isEqualTo(at2Latest.getForecastedAt());

        assertThat(list.get(4).getForecastAt()).isEqualTo(at3);
        assertThat(list.get(5).getForecastAt()).isEqualTo(at3);
        assertThat(list.get(4).getForecastedAt()).isEqualTo(at3Latest.getForecastedAt());
    }
}
