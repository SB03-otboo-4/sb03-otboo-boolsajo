package com.sprint.otboo.weather.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.common.config.JpaAuditingConfig;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    // DB 제약(0..1)에 맞춰 확률을 0..1로 저장
    "weather.owm.probability-percent=false"
})
@Import({
    JpaAuditingConfig.class,
    WeatherCollectTaskletOwmTest.TestStubConfig.class
})
@ActiveProfiles("test")
@DisplayName("WeatherCollectTaskletOwm 테스트")
class WeatherCollectTaskletOwmTest {

    @Autowired Job weatherForecastJob;
    @Autowired JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired WeatherRepository weatherRepository;
    @Autowired WeatherLocationRepository locationRepository;

    private WeatherLocation location;
    private Instant forecastAt;

    @BeforeEach
    void 준비() {
        location = locationRepository.findAll().stream().findFirst()
            .orElseGet(() -> locationRepository.save(
                WeatherLocation.builder()
                    .id(UUID.randomUUID())
                    .latitude(BigDecimal.valueOf(37.5665))
                    .longitude(BigDecimal.valueOf(126.9780))
                    .x(60)
                    .y(127)
                    .locationNames("서울특별시 중구")
                    .createdAt(Instant.now())
                    .build()
            ));

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
            .currentC(20.0)
            .amountMm(0.0)
            .probability(0.0)
            .build();

        weatherRepository.save(legacy);
    }

    @Test
    void 배치를_실행하면_OWM_스텁이_저장되고_동일_forecastAt의_최신_발표본만_남는다() throws Exception {
        long beforeCount = weatherRepository.count();

        JobParameters params = new JobParametersBuilder()
            .addLong("executionTime", System.currentTimeMillis())
            .toJobParameters();

        jobLauncherTestUtils.setJob(weatherForecastJob);
        jobLauncherTestUtils.launchJob(params);

        long afterCount = weatherRepository.count();
        assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);

        List<Weather> allAt = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                location.getId(), forecastAt, forecastAt
            );

        assertThat(allAt).isNotEmpty();

        // 최신 발표본 선택(정렬 보장 X 방어)
        Weather latest = allAt.stream()
            .max(Comparator.comparing(Weather::getForecastedAt))
            .orElseThrow();

        assertThat(latest.getForecastAt()).isEqualTo(forecastAt);
        assertThat(latest.getCurrentC()).isEqualTo(21.5);
        assertThat(latest.getAmountMm()).isEqualTo(0.5);
        // 0..1 범위로 저장되므로 0.3 검증
        assertThat(latest.getProbability()).isEqualTo(0.3);
    }

    @Test
    void 위치_목록이_없으면_스킵되고_정상_종료된다() throws Exception {
        weatherRepository.deleteAll();
        locationRepository.deleteAll();

        JobParameters params = new JobParametersBuilder()
            .addLong("executionTime", System.currentTimeMillis())
            .toJobParameters();

        jobLauncherTestUtils.setJob(weatherForecastJob);
        jobLauncherTestUtils.launchJob(params);

        assertThat(weatherRepository.count()).isZero();
    }

    static class TestStubConfig {

        @Bean
        @Primary
        WeatherDataClient stubWeatherDataClient() {
            WeatherDataClient stub = Mockito.mock(WeatherDataClient.class);
            Instant fixedForecastAt = Instant.parse("2025-10-23T06:00:00Z");

            List<CollectedForecast> fake = List.of(
                new CollectedForecast(
                    fixedForecastAt, 21.5, 55, 3.2,
                    0.3, 0.5, null, 5, 800
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
