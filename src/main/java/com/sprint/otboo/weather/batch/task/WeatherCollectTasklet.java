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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WeatherCollectTasklet implements Tasklet {

    private final WeatherLocationRepository locationRepository;
    private final WeatherRepository weatherRepository;
    private final KmaRequestBuilder kmaRequestBuilder;
    private final KmaShortTermForecastClient kmaClient;
    private final KmaForecastAssembler kmaAssembler;
    private final RetryTemplate weatherRetryTemplate;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<WeatherLocation> locations = locationRepository.findAll();
        for (WeatherLocation loc : locations) {
            double lat = loc.getLatitude().doubleValue();
            double lon = loc.getLongitude().doubleValue();

            Map<String, String> params = kmaRequestBuilder.toParams(lat, lon, Instant.now());

            KmaForecastResponse response = weatherRetryTemplate.execute(ctx -> kmaClient.getVilageFcst(params));

            List<KmaForecastMapper.Slot> slots = kmaAssembler.toSlots(response.getItems());
            List<Weather> snapshots = kmaAssembler.toWeathers(slots, loc);

            for (Weather w : snapshots) {
                UUID locationId = loc.getId();
                Optional<Weather> exists = weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
                    locationId, w.getForecastAt(), w.getForecastedAt()
                );
                if (exists.isEmpty()) {
                    weatherRepository.save(w);
                }
            }
        }
        return RepeatStatus.FINISHED;
    }
}
