package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaRequestBuilder;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.mapper.KmaForecastAssembler;
import com.sprint.otboo.weather.mapper.KmaForecastMapper.Slot;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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

        // 1) 위치 조회 (반드시 엔티티 먼저 확보: 캐시 폴백 시에도 필요)
        WeatherLocationResponse locDto = locationQueryService.getWeatherLocation(latitude, longitude);
        WeatherLocation location = resolveLocationEntity(locDto);

        // 2) KMA 요청 파라미터
        Instant now = Instant.now();
        Map<String, String> params = kmaRequestBuilder.toParams(locDto.latitude(), locDto.longitude(), now);

        try {
            // 3) KMA 호출 → 슬롯/스냅샷 변환
            KmaForecastResponse response = kmaClient.getVilageFcst(params);
            List<Slot> slots = kmaAssembler.toSlots(response.getItems());
            List<Weather> snapshots = kmaAssembler.toWeathers(slots, location);

            // 4) 중복 방지 저장
            for (Weather w : snapshots) {
                UUID locationId = location.getId();
                boolean exists = weatherRepository
                    .findByLocationIdAndForecastAtAndForecastedAt(locationId, w.getForecastAt(), w.getForecastedAt())
                    .isPresent();
                if (!exists) {
                    try { weatherRepository.save(w); }
                    catch (DataIntegrityViolationException ignore) {
                        // 동시 저장 경합 또는 유니크 충돌 → 무시
                    }
                }
            }

            // 5) 응답 매핑
            return snapshots.stream()
                .map(weatherMapper::toFeedWeatherDto)
                .toList();

        } catch (RuntimeException upstream) {
            // 6) 폴백: 캐시에서 최신 72시간 범위를 가져와 "forecastAt별 가장 최신값"을 리턴
            log.warn("KMA upstream timeout/IO error. Falling back to cached data. x={}, y={}",
                locDto.x(), locDto.y(), upstream);

            Instant from = now.minus(Duration.ofHours(1));   // 직전 1시간 포함
            Instant to   = now.plus(Duration.ofHours(72));    // 앞으로 72시간

            List<Weather> cached = weatherRepository.findRangeOrdered(location.getId(), from, to);
            if (!cached.isEmpty()) {
                // repository 정렬: forecast_at ASC, forecasted_at DESC
                // → 같은 forecast_at 내에서 첫 번째 레코드가 "가장 최신 예보"
                List<Weather> latestPerForecastAt = pickLatestPerForecastAt(cached);

                return latestPerForecastAt.stream()
                    .map(weatherMapper::toFeedWeatherDto)
                    .collect(Collectors.toList());
            }

            // 캐시에도 아무것도 없으면 그대로 예외 전파 → 컨트롤러 어드바이스에서 처리
            throw upstream;
        }
    }

    private static List<Weather> pickLatestPerForecastAt(List<Weather> ordered) {
        List<Weather> result = new ArrayList<>();
        Instant currentKey = null;
        for (Weather w : ordered) {
            if (!Objects.equals(currentKey, w.getForecastAt())) {
                result.add(w);
                currentKey = w.getForecastAt();
            }
        }
        return result;
    }

    private WeatherLocation resolveLocationEntity(WeatherLocationResponse dto) {
        Optional<WeatherLocation> byXY = locationRepository.findFirstByXAndY(dto.x(), dto.y());
        if (byXY.isPresent()) return byXY.get();

        return locationRepository.findFirstByLatitudeAndLongitude(
            BigDecimal.valueOf(dto.latitude()),
            BigDecimal.valueOf(dto.longitude())
        ).orElseThrow(() -> new IllegalStateException("WeatherLocation entity not found"));
    }
}
