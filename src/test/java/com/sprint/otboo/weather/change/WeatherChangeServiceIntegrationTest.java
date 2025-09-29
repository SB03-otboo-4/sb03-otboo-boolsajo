package com.sprint.otboo.weather.change;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("특이 기상 변화 서비스 통합 테스트")
class WeatherChangeServiceIntegrationTest {

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private WeatherLocationRepository locationRepository;

    @Autowired
    private WeatherChangeRepository changeRepository;

    @Autowired
    private WeatherChangeService changeService;

    @BeforeEach
    void 데이터_정리() {
        changeRepository.deleteAll();
        weatherRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    void 최신_2개를_비교해_변화를_저장하고_중복_저장하지_않는다() {
        WeatherLocation loc = WeatherLocation.builder().build();
        loc.setId(UUID.randomUUID());
        loc.setLatitude(new BigDecimal("37.5665"));
        loc.setLongitude(new BigDecimal("126.9780"));
        loc.setX(60); loc.setY(127);
        locationRepository.save(loc);

        Instant forecastAt = Instant.parse("2025-09-28T00:00:00Z");

        Weather older = Weather.builder()
            .location(loc)
            .forecastAt(forecastAt)
            .forecastedAt(Instant.parse("2025-09-27T23:00:00Z"))
            .skyStatus(SkyStatus.CLEAR)
            .type(PreciptationType.NONE)
            .currentC(22.0)
            .probability(10.0)
            .build();

        Weather newer = Weather.builder()
            .location(loc)
            .forecastAt(forecastAt)
            .forecastedAt(Instant.parse("2025-09-28T02:00:00Z"))
            .skyStatus(SkyStatus.CLOUDY)
            .type(PreciptationType.RAIN) // 변화
            .currentC(21.0)
            .probability(60.0)
            .build();

        weatherRepository.saveAll(List.of(older, newer));

        changeService.process(loc.getId(), forecastAt);

        long first = changeRepository.count();
        assertThat(first).isGreaterThan(0);

        changeService.process(loc.getId(), forecastAt);
        assertThat(changeRepository.count()).isEqualTo(first);
    }
}
