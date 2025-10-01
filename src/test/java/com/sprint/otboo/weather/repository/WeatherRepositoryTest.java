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
import java.util.Optional;
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

    @Test
    void 정확_매칭으로_단일_스냅샷을_조회할_수_있다() {
        WeatherLocation location = saveLocation(37.5665, 126.9780, 60, 127, "서울특별시 중구 태평로1가");

        Instant forecastAt = Instant.parse("2025-09-25T09:00:00Z");
        Instant released1 = Instant.parse("2025-09-25T08:30:00Z");
        Instant released2 = Instant.parse("2025-09-25T08:40:00Z");

        saveWeather(location, forecastAt, released1);
        Weather target = saveWeather(location, forecastAt, released2);

        Optional<Weather> found = weatherRepository
            .findByLocationIdAndForecastAtAndForecastedAt(location.getId(), forecastAt, released2);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(target.getId());
    }

    @Test
    void 정확_매칭에서_존재하지_않으면_빈값을_반환한다() {
        WeatherLocation location = saveLocation(35.1796, 129.0756, 98, 76, "부산광역시 중구 중앙동");

        Optional<Weather> found = weatherRepository
            .findByLocationIdAndForecastAtAndForecastedAt(
                location.getId(),
                Instant.parse("2025-09-25T00:00:00Z"),
                Instant.parse("2025-09-24T23:30:00Z")
            );
        assertThat(found).isNotPresent();
    }

    @Test
    void 기간_조회에서_between은_경계를_포함한다() {
        WeatherLocation location = saveLocation(37.4000, 127.1000, 62, 123, "경기도 성남시 분당구");

        Instant from = Instant.parse("2025-09-25T00:00:00Z");
        Instant mid  = Instant.parse("2025-09-25T03:00:00Z");
        Instant to   = Instant.parse("2025-09-25T06:00:00Z");

        // 각 forecastAt에 1건만 넣어도 정렬·포함성 검증에 충분
        saveWeather(location, from, Instant.parse("2025-09-24T23:40:00Z"));
        saveWeather(location, mid,  Instant.parse("2025-09-25T02:40:00Z"));
        saveWeather(location, to,   Instant.parse("2025-09-25T05:40:00Z"));

        List<Weather> list = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                location.getId(), from, to);

        // from과 to가 포함되는지 확인 (총 3건)
        assertThat(list).hasSize(3);
        assertThat(list.get(0).getForecastAt()).isEqualTo(from);
        assertThat(list.get(1).getForecastAt()).isEqualTo(mid);
        assertThat(list.get(2).getForecastAt()).isEqualTo(to);
    }

    @Test
    void 최신2건_조회에서_데이터가_1개뿐이어도_안전하게_동작한다() {
        WeatherLocation location = saveLocation(35.1000, 129.0200, 97, 75, "부산광역시 서구");

        Instant at = Instant.parse("2025-09-25T12:00:00Z");
        Weather only = saveWeather(location, at, Instant.parse("2025-09-25T11:40:00Z"));

        List<Weather> top2 = weatherRepository
            .findTop2ByLocationIdAndForecastAtOrderByForecastedAtDesc(location.getId(), at);

        assertThat(top2).hasSize(1);
        assertThat(top2.get(0).getId()).isEqualTo(only.getId());
    }

    @Test
    void 서로_다른_location의_데이터는_섞이지_않는다() {
        WeatherLocation seoul = saveLocation(37.5665, 126.9780, 60, 127, "서울특별시 중구");
        WeatherLocation busan = saveLocation(35.1796, 129.0756, 98, 76, "부산광역시 중구");

        Instant at = Instant.parse("2025-09-25T15:00:00Z");
        // 서울 2건
        saveWeather(seoul, at, Instant.parse("2025-09-25T14:10:00Z"));
        saveWeather(seoul, at, Instant.parse("2025-09-25T14:40:00Z"));
        // 부산 2건
        saveWeather(busan, at, Instant.parse("2025-09-25T14:20:00Z"));
        saveWeather(busan, at, Instant.parse("2025-09-25T14:50:00Z"));

        List<Weather> seoulOnly = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                seoul.getId(), at, at);

        assertThat(seoulOnly).hasSize(2);
        assertThat(seoulOnly.stream().allMatch(w -> w.getLocation().getId().equals(seoul.getId())))
            .isTrue();
    }

    @Test
    void 기본_메서드_findLatest2는_원본_쿼리와_동일하게_동작한다() {
        WeatherLocation location = saveLocation(37.1234, 127.5678, 61, 125, "경기도 용인시");

        Instant at = Instant.parse("2025-09-25T18:00:00Z");
        saveWeather(location, at, Instant.parse("2025-09-25T17:10:00Z"));
        Weather second = saveWeather(location, at, Instant.parse("2025-09-25T17:40:00Z"));
        Weather latest = saveWeather(location, at, Instant.parse("2025-09-25T17:55:00Z"));

        List<Weather> byOrigin = weatherRepository
            .findTop2ByLocationIdAndForecastAtOrderByForecastedAtDesc(location.getId(), at);
        List<Weather> byDefault = weatherRepository.findLatest2(location.getId(), at);

        assertThat(byDefault).hasSize(2);
        assertThat(byDefault.get(0).getId()).isEqualTo(byOrigin.get(0).getId());
        assertThat(byDefault.get(1).getId()).isEqualTo(byOrigin.get(1).getId());
        assertThat(byDefault.get(0).getId()).isEqualTo(latest.getId());
        assertThat(byDefault.get(1).getId()).isEqualTo(second.getId());
    }

    @Test
    void 기본_메서드_findRangeOrdered는_원본_쿼리와_동일하게_동작한다() {
        WeatherLocation location = saveLocation(37.5010, 127.0400, 61, 125, "서울특별시 강남구");

        Instant at1 = Instant.parse("2025-09-25T00:00:00Z");
        Instant at2 = Instant.parse("2025-09-25T03:00:00Z");

        Weather at1Old = saveWeather(location, at1, Instant.parse("2025-09-24T23:10:00Z"));
        Weather at1New = saveWeather(location, at1, Instant.parse("2025-09-24T23:40:00Z"));
        Weather at2Old = saveWeather(location, at2, Instant.parse("2025-09-25T02:10:00Z"));
        Weather at2New = saveWeather(location, at2, Instant.parse("2025-09-25T02:40:00Z"));

        List<Weather> byOrigin = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                location.getId(), at1, at2);
        List<Weather> byDefault = weatherRepository.findRangeOrdered(location.getId(), at1, at2);

        assertThat(byDefault).hasSize(4);
        // 정렬 동일
        for (int i = 0; i < byOrigin.size(); i++) {
            assertThat(byDefault.get(i).getId()).isEqualTo(byOrigin.get(i).getId());
        }
        // 그룹 내 최신 우선
        assertThat(byDefault.get(0).getForecastedAt()).isEqualTo(at1New.getForecastedAt());
        assertThat(byDefault.get(1).getForecastedAt()).isEqualTo(at1Old.getForecastedAt());
        assertThat(byDefault.get(2).getForecastedAt()).isEqualTo(at2New.getForecastedAt());
        assertThat(byDefault.get(3).getForecastedAt()).isEqualTo(at2Old.getForecastedAt());
    }
}
