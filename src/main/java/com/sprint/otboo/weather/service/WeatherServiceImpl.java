package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaRequestBuilder;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.mapper.KmaForecastAssembler;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherLocationQueryService locationQueryService;
    private final WeatherLocationRepository locationRepository;
    private final KmaRequestBuilder kmaRequestBuilder;
    private final KmaShortTermForecastClient kmaClient;
    private final KmaForecastAssembler kmaAssembler;
    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;

    @Override
    @Transactional
    public List<WeatherSummaryDto> getWeather(Double latitude, Double longitude) {

        WeatherLocationResponse locDto = locationQueryService.getWeatherLocation(latitude, longitude);

        Map<String, String> params = kmaRequestBuilder.toParams(
            locDto.latitude(), locDto.longitude(), Instant.now()
        );

        KmaForecastResponse response = kmaClient.getVilageFcst(params);

        WeatherLocation location = resolveLocationEntity(locDto);

        List<com.sprint.otboo.weather.mapper.KmaForecastMapper.Slot> slots = kmaAssembler.toSlots(response.getItems());
        List<Weather> snapshots = kmaAssembler.toWeathers(slots, location);

        for (Weather w : snapshots) {
            UUID locationId = location.getId();
            Optional<Weather> exists = weatherRepository
                .findByLocationIdAndForecastAtAndForecastedAt(
                    locationId, w.getForecastAt(), w.getForecastedAt()
                );
            if (exists.isEmpty()) {
                weatherRepository.save(w);
            }
        }

        return snapshots.stream()
            .map(weatherMapper::toFeedWeatherDto)
            .toList();
    }

    private WeatherLocation resolveLocationEntity(WeatherLocationResponse dto) {
        Optional<WeatherLocation> byXY = locationRepository.findFirstByXAndY(dto.x(), dto.y());
        if (byXY.isPresent()) return byXY.get();

        // double -> BigDecimal 변환 ⬅️ (시그니처 맞추기)
        return locationRepository.findFirstByLatitudeAndLongitude(
            BigDecimal.valueOf(dto.latitude()),
            BigDecimal.valueOf(dto.longitude())
        ).orElseThrow(() -> new IllegalStateException("WeatherLocation entity not found"));
    }
}
