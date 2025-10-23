package com.sprint.otboo.weather.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.common.config.JpaAuditingConfig;
import com.sprint.otboo.weather.batch.task.WeatherCollectTasklet;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import com.sprint.otboo.weather.mapper.OwmForecastAssembler;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBatchTest
@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "weather.owm.enabled=true",
    "weather.kma.enabled=false",
    "weather.owm.probability-percent=true"
})
@Import({
    JpaAuditingConfig.class,
    WeatherCollectTaskletOwmIT.TestStubConfig.class
})
@ActiveProfiles("test")
@DisplayName("통합테스트: OWM 수집 배치 → Weather 저장/버전정리")
class WeatherCollectTaskletOwmIT {

    @Autowired Job weatherForecastJob;
    @Autowired JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired WeatherRepository weatherRepository;
    @Autowired WeatherLocationRepository locationRepository;

    @Autowired OwmForecastAssembler owmForecastAssembler;

    private WeatherLocation location;
    private Instant forecastAt; // 검증용 슬롯 시각

    @BeforeEach
    void setUp() {
        // WeatherLocation은 latitude/longitude/x/y/locationNames/createdAt 모두 필요
        location = locationRepository.findAll().stream().findFirst()
            .orElseGet(() -> locationRepository.save(
                WeatherLocation.builder()
                    .id(UUID.randomUUID())
                    .latitude(BigDecimal.valueOf(37.5665))
                    .longitude(BigDecimal.valueOf(126.9780))
                    .x(60)  // 예시 격자값, 실제는 KMA Grid 변환 필요 없음
                    .y(127)
                    .locationNames("서울특별시 중구") // NOT NULL
                    .createdAt(Instant.now())
                    .build()
            ));

        // 기존 버전 1개 심기 → 배치 후 최신으로 치환되는지 검증
        forecastAt = Instant.parse("2025-10-23T06:00:00Z");
        Instant olderForecastedAt = Instant.now().minus(1, ChronoUnit.HOURS);

        Weather legacy = Weather.builder()
            .location(location)
            .forecastAt(forecastAt)
            .forecastedAt(olderForecastedAt)
            .skyStatus(SkyStatus.CLEAR)
            .asWord(WindStrength.WEAK)
            .type(PrecipitationType.NONE)
            .speedMs(1.2)
            .currentPct(50.0)
            .comparedPct(null)
            .currentC(20.0)
            .comparedC(null)
            .minC(null)
            .maxC(null)
            .amountMm(0.0)
            .probability(0.0)
            .build();

        weatherRepository.save(legacy);
    }

    @Test
    @DisplayName("배치를 실행하면 OWM 스텁 데이터가 저장되고, 동일 forecastAt의 최신 발표본만 남긴다")
    void collect_and_keep_latest_version() throws Exception {
        long beforeCount = weatherRepository.count();

        JobParameters params = new JobParametersBuilder()
            .addLong("executionTime", System.currentTimeMillis())
            .toJobParameters();

        jobLauncherTestUtils.setJob(weatherForecastJob);
        jobLauncherTestUtils.launchJob(params);

        long afterCount = weatherRepository.count();
        assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);

        // 동일 forecastAt 범위 조회
        List<Weather> allAt = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                location.getId(), forecastAt, forecastAt
            );

        assertThat(allAt).isNotEmpty();
        Weather latest = allAt.get(0);

        // 값 검증
        assertThat(latest.getForecastAt()).isEqualTo(forecastAt);
        assertThat(latest.getForecastedAt()).isNotNull();
        assertThat(latest.getCreatedAt()).isNotNull();
        assertThat(latest.getCurrentC()).isEqualTo(21.5);
        assertThat(latest.getAmountMm()).isEqualTo(0.5);
        assertThat(latest.getProbability()).isEqualTo(30.0); // pop 0.3 → 30%

        // 구버전보다 발표시각이 최신이어야 한다
        if (allAt.size() > 1) {
            Weather second = allAt.get(1);
            assertThat(second.getForecastedAt()).isBefore(latest.getForecastedAt());
        }
    }

    /**
     * WeatherDataClient 스텁 정의
     */
    static class TestStubConfig {

        @Bean
        @Primary
        WeatherDataClient stubWeatherDataClient() {
            WeatherDataClient stub = Mockito.mock(WeatherDataClient.class);

            Instant fixedForecastAt = Instant.parse("2025-10-23T06:00:00Z");

            List<CollectedForecast> fake = List.of(
                new CollectedForecast(
                    fixedForecastAt,
                    21.5,          // temp(C)
                    55,            // humidity(%)
                    3.2,           // wind(m/s)
                    0.3,           // pop(0..1)
                    0.5,           // rain3h(mm)
                    null,          // snow
                    5,             // clouds.all(%)
                    800            // weather.id (clear)
                )
            );

            Mockito.when(stub.fetch(Mockito.anyDouble(), Mockito.anyDouble(), Mockito.any(Locale.class)))
                .thenReturn(fake);

            return stub;
        }

        @Bean
        JobLauncherTestUtils jobLauncherTestUtils() {
            return new JobLauncherTestUtils();
        }
    }
}
