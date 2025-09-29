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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        long epoch = resolveExecutionTime(chunkContext);
        Instant at = Instant.ofEpochMilli(epoch);

        int saved = 0;
        int skipped = 0;

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

                List<KmaForecastMapper.Slot> slots = kmaAssembler.toSlots(response.getItems());
                List<Weather> snapshots = kmaAssembler.toWeathers(slots, loc);

                for (Weather w : snapshots) {
                    UUID locationId = loc.getId();
                    Optional<Weather> exists = weatherRepository
                        .findByLocationIdAndForecastAtAndForecastedAt(
                            locationId, w.getForecastAt(), w.getForecastedAt()
                        );
                    if (exists.isEmpty()) {
                        weatherRepository.save(w);
                        saved++;
                    }
                }
            } catch (Exception ex) {
                log.warn("[weather-batch] location {} failed: {}", loc.getId(), ex.toString());
            }
        }

        log.info("[weather-batch] processed locations={}, saved={}, skipped={}", locations.size(), saved, skipped);
        return RepeatStatus.FINISHED;
    }

    private long resolveExecutionTime(ChunkContext chunkContext) {
        Object value = chunkContext.getStepContext().getJobParameters().get("executionTime");
        if (value instanceof Long) {
            return (Long) value;
        }
        // 파라미터 없으면 현재 시각 사용
        return System.currentTimeMillis();
    }
}
