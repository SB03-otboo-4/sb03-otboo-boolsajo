package com.sprint.otboo.weather.batch.task;

import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaRequestBuilder;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.mapper.KmaForecastAssembler;
import com.sprint.otboo.weather.mapper.KmaForecastMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    private final KmaRequestBuilder kmaRequestBuilder;
    private final KmaShortTermForecastClient kmaClient;
    private final KmaForecastAssembler kmaAssembler;
    private final RetryTemplate weatherRetryTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<WeatherLocation> locations = locationRepository.findAll();

        if (locations.isEmpty()) {
            log.info("[weather-batch] no locations to process");
            return RepeatStatus.FINISHED;
        }

        Instant at = Instant.ofEpochMilli(resolveExecutionTime(chunkContext));
        int totalSaved = 0, skipped = 0;

        for (WeatherLocation loc : locations) {
            if (loc.getLatitude() == null || loc.getLongitude() == null) {
                skipped++;
                log.warn("[weather-batch] skip location {} due to null lat/lon", loc.getId());
                continue;
            }
            try {
                Map<String, String> params = kmaRequestBuilder.toParams(
                    loc.getLatitude().doubleValue(),
                    loc.getLongitude().doubleValue(),
                    at
                );

                KmaForecastResponse response = weatherRetryTemplate.execute(ctx ->
                    kmaClient.getVilageFcst(params)
                );

                // PCP/TMN/TMX 반영 호출
                List<KmaForecastMapper.Slot> slots = kmaAssembler.toSlots(response.getItems());
                List<Weather> snapshots = kmaAssembler.toWeathers(slots, loc, response.getItems());
                if (snapshots.isEmpty()) continue;

                // 범위 계산
                Instant minAt = snapshots.stream().map(Weather::getForecastAt).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
                Instant maxAt = snapshots.stream().map(Weather::getForecastAt).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null);

                // N+1 방지: 해당 범위 기존 키 한 번에 로드
                List<Weather> existed = weatherRepository
                    .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
                        loc.getId(), minAt, maxAt
                    );
                Set<String> existedKeys = existed.stream()
                    .map(w -> key(w.getForecastAt(), w.getForecastedAt()))
                    .collect(Collectors.toSet());

                // 신규만 선별
                List<Weather> toSave = snapshots.stream()
                    .filter(w -> !existedKeys.contains(key(w.getForecastAt(), w.getForecastedAt())))
                    .toList();

                if (!toSave.isEmpty()) {
                    weatherRepository.saveAll(toSave);
                    totalSaved += toSave.size();

                    // 저장한 범위에서 최신 발표본만 남기고 정리
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
