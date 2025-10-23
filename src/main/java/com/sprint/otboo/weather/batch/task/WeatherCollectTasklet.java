package com.sprint.otboo.weather.batch.task;

import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import com.sprint.otboo.weather.integration.owm.mapper.OwmForecastAssembler;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class WeatherCollectTasklet implements Tasklet {

    private final WeatherLocationRepository locationRepository;
    private final WeatherRepository weatherRepository;

    private final WeatherDataClient dataClient;          // OWM 등 수집기 (SPI)
    private final OwmForecastAssembler owmAssembler;     // OWM 규칙 적용 엔티티 변환기
    private final RetryTemplate weatherRetryTemplate;    // 재시도 로직 재사용

    private final Locale owmDefaultLocale = Locale.KOREAN;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<WeatherLocation> locations = locationRepository.findAll();
        if (locations.isEmpty()) {
            log.info("[weather-batch] no locations to process");
            return RepeatStatus.FINISHED;
        }

        // 실행 기준시각(예보 대상시각 선택 보정용). 기존 파라미터 유지
        Instant at = Instant.ofEpochMilli(resolveExecutionTime(chunkContext));
        int totalSaved = 0, skipped = 0;

        // 수집(발표/산출) 시각 → forecastedAt 대체
        Instant ingestedAt = Instant.now();

        for (WeatherLocation loc : locations) {
            if (loc.getLatitude() == null || loc.getLongitude() == null) {
                skipped++;
                log.warn("[weather-batch] skip location {} due to null lat/lon", loc.getId());
                continue;
            }
            try {
                // OWM 수집 (재시도 포함)
                List<CollectedForecast> collected = weatherRetryTemplate.execute(ctx ->
                    dataClient.fetch(loc.getLatitude().doubleValue(),
                        loc.getLongitude().doubleValue(),
                        owmDefaultLocale)
                );
                if (collected == null || collected.isEmpty()) continue;

                // OWM → Weather 엔티티로 변환
                List<Weather> snapshots = collected.stream()
                    .map(cf -> owmAssembler.toEntity(loc, cf, ingestedAt))
                    .toList();
                if (snapshots.isEmpty()) continue;

                // 범위 계산 (forecastAt 기준)
                Instant minAt = snapshots.stream().map(Weather::getForecastAt).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
                Instant maxAt = snapshots.stream().map(Weather::getForecastAt).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null);

                if (minAt == null || maxAt == null) continue;

                // 범위 내 기존 데이터 한 번에 적재 (N+1 방지)
                List<Weather> existed = weatherRepository
                    .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                        loc.getId(), minAt, maxAt
                    );
                Set<String> existedKeys = existed.stream()
                    .map(w -> key(w.getForecastAt(), w.getForecastedAt()))
                    .collect(Collectors.toSet());

                // 신규만 저장
                List<Weather> toSave = snapshots.stream()
                    .filter(w -> !existedKeys.contains(key(w.getForecastAt(), w.getForecastedAt())))
                    .toList();

                if (!toSave.isEmpty()) {
                    weatherRepository.saveAll(toSave);
                    totalSaved += toSave.size();

                    // 동일 forecastAt에 대해 최신 발표본만 유지
                    weatherRepository.deleteOlderVersionsInRange(loc.getId(), minAt, maxAt);
                }

            } catch (Exception ex) {
                log.warn("[weather-batch] location {} failed: {}", loc.getId(), ex.toString());
            }
        }

        log.info("[weather-batch] processed locations={}, saved={}, skipped={}",
            locations.size(), totalSaved, skipped);
        return RepeatStatus.FINISHED;
    }

    private long resolveExecutionTime(ChunkContext chunkContext) {
        Object value = chunkContext.getStepContext().getJobParameters().get("executionTime");
        return (value instanceof Long) ? (Long) value : System.currentTimeMillis();
    }

    private static String key(Instant forecastAt, Instant forecastedAt) {
        return forecastAt.toString() + "|" + (forecastedAt != null ? forecastedAt.toString() : "");
    }
}
