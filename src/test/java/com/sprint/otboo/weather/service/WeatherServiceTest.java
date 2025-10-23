package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.weather.dto.data.WeatherDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaRequestBuilder;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.integration.kma.mapper.KmaForecastAssembler;
import com.sprint.otboo.weather.integration.kma.mapper.KmaForecastMapper;
import com.sprint.otboo.weather.integration.kma.mapper.KmaForecastMapper.Slot;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService 테스트")
class WeatherServiceTest {

    @Mock private WeatherLocationQueryService locationQueryService;
    @Mock private WeatherLocationRepository locationRepository;
    @Mock private WeatherRepository weatherRepository;
    @Mock private WeatherMapper weatherMapper;
    @Mock private WeatherDataClient weatherDataClient; // OWM 목

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Test
    void 위경도_입력시_OWM이_비면_캐시로_폴백하여_DTO_1건을_반환한다() {
        // given: 위치/좌표
        double lat = 37.5665, lon = 126.9780;
        when(locationQueryService.getWeatherLocation(lat, lon))
            .thenReturn(new WeatherLocationResponse(lat, lon, 60, 127, List.of("서울특별시", "중구")));

        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setX(60); location.setY(127);
        when(locationRepository.findFirstByXAndY(60, 127)).thenReturn(Optional.of(location));

        // OWM → 빈 목록(폴백 유도)
        when(weatherDataClient.fetch(eq(lat), eq(lon), any())).thenReturn(List.of());

        // 캐시(저장소)에서 최근 범위 1건 반환
        Instant now = Instant.now();
        Weather cached = Weather.builder()
            .location(location)
            .forecastAt(now.plusSeconds(3600))
            .forecastedAt(now)
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .currentC(22.0)
            .probability(10.0)
            .build();
        when(weatherRepository.findRangeOrdered(eq(location.getId()), any(), any()))
            .thenReturn(List.of(cached));

        when(weatherMapper.toWeatherDto(any(Weather.class)))
            .thenReturn(new WeatherDto(UUID.randomUUID(), now, now, null, "CLEAR", null, null, null, null));

        // when
        List<WeatherDto> out = weatherService.getWeather(lat, lon);

        // then
        assertThat(out).hasSize(1);
    }

    @Test
    void 동일_스냅샷이_이미_존재해도_캐시_폴백으로_DTO_1건을_반환한다() {
        // given: 위치/좌표
        double lat = 35.1796, lon = 129.0756;
        when(locationQueryService.getWeatherLocation(lat, lon))
            .thenReturn(new WeatherLocationResponse(lat, lon, 98, 76, List.of("부산광역시", "중구")));

        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setX(98); location.setY(76);
        when(locationRepository.findFirstByXAndY(98, 76)).thenReturn(Optional.of(location));

        // OWM → 빈 목록(항상 폴백)
        when(weatherDataClient.fetch(eq(lat), eq(lon), any())).thenReturn(List.of());

        // 캐시에서 1건 반환
        Instant now = Instant.now();
        Weather cached = Weather.builder()
            .location(location)
            .forecastAt(now.plusSeconds(7200))
            .forecastedAt(now.plusSeconds(300))
            .skyStatus(SkyStatus.MOSTLY_CLOUDY)
            .type(PrecipitationType.NONE)
            .currentC(24.0)
            .probability(0.0)
            .build();
        when(weatherRepository.findRangeOrdered(eq(location.getId()), any(), any()))
            .thenReturn(List.of(cached));

        when(weatherMapper.toWeatherDto(any(Weather.class)))
            .thenReturn(new WeatherDto(UUID.randomUUID(), now, now, null, "CLOUDY", null, null, null, null));

        // when
        List<WeatherDto> out = weatherService.getWeather(lat, lon);

        // then
        assertThat(out).hasSize(1);
    }
}
