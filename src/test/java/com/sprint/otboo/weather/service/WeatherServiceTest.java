package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaRequestBuilder;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.mapper.KmaForecastAssembler;
import com.sprint.otboo.weather.mapper.KmaForecastMapper;
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
    @Mock private KmaRequestBuilder kmaRequestBuilder;
    @Mock private KmaShortTermForecastClient kmaClient;
    @Mock private KmaForecastAssembler kmaAssembler;
    @Mock private WeatherRepository weatherRepository;
    @Mock private WeatherMapper weatherMapper;

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Test
    void 위경도_입력시_파라미터를_빌드하여_KMA호출_후_슬롯을_엔티티로_업서트하고_DTO로_반환한다() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;

        WeatherLocationResponse locDto = new WeatherLocationResponse(
            37.5665, 126.9780, 60, 127, java.util.List.of("서울특별시", "중구")
        );
        when(locationQueryService.getWeatherLocation(latitude, longitude)).thenReturn(locDto);

        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setX(60); location.setY(127);
        when(locationRepository.findFirstByXAndY(60, 127)).thenReturn(Optional.of(location));

        Map<String, String> params = new LinkedHashMap<>();
        params.put("nx", "60"); params.put("ny", "127");

        when(kmaRequestBuilder.toParams(eq(latitude), eq(longitude), any(Instant.class))).thenReturn(params);

        KmaForecastResponse resp = new KmaForecastResponse();
        resp.setItems(List.of(new KmaForecastItem()));
        when(kmaClient.getVilageFcst(params)).thenReturn(resp);

        KmaForecastMapper.Slot slot = new KmaForecastMapper.Slot(
            "20250928","0900", SkyStatus.CLEAR, PrecipitationType.NONE, 22, 60, 10
        );
        when(kmaAssembler.toSlots(resp.getItems())).thenReturn(List.of(slot));

        Weather snapshot = Weather.builder()
            .location(location)
            .forecastAt(Instant.parse("2025-09-28T00:00:00Z"))
            .forecastedAt(Instant.parse("2025-09-27T23:30:00Z"))
            .skyStatus(SkyStatus.CLEAR)
            .type(PrecipitationType.NONE)
            .currentC(22.0)
            .probability(10.0)
            .build();
        when(kmaAssembler.toWeathers(List.of(slot), location)).thenReturn(List.of(snapshot));

        when(weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
            location.getId(), snapshot.getForecastAt(), snapshot.getForecastedAt()
        )).thenReturn(Optional.empty());
        when(weatherRepository.save(any(Weather.class))).thenAnswer(inv -> inv.getArgument(0));

        WeatherSummaryDto dto = new WeatherSummaryDto(
            UUID.randomUUID(), "CLEAR",
            new PrecipitationDto("NONE", 0.0, 10.0),
            new TemperatureDto(22.0, 0.0, 0.0, 0.0)
        );
        when(weatherMapper.toFeedWeatherDto(snapshot)).thenReturn(dto);

        // when
        List<WeatherSummaryDto> result = weatherService.getWeather(latitude, longitude);

        // then
        verify(kmaClient).getVilageFcst(params);
        verify(weatherRepository).save(any(Weather.class));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).skyStatus()).isEqualTo("CLEAR");
    }

    @Test
    void 동일_스냅샷이_이미_존재하면_저장을_생략하고_DTO만_반환한다() {
        // given
        Double latitude = 35.1796;
        Double longitude = 129.0756;

        WeatherLocationResponse locDto = new WeatherLocationResponse(35.1796,129.0756,98,76, java.util.List.of());
        when(locationQueryService.getWeatherLocation(latitude, longitude)).thenReturn(locDto);

        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setX(98); location.setY(76);
        when(locationRepository.findFirstByXAndY(98, 76)).thenReturn(Optional.of(location));

        Map<String, String> params = new LinkedHashMap<>();

        when(kmaRequestBuilder.toParams(eq(latitude), eq(longitude), any(Instant.class))).thenReturn(params);

        KmaForecastResponse resp = new KmaForecastResponse();
        resp.setItems(List.of(new KmaForecastItem()));
        when(kmaClient.getVilageFcst(params)).thenReturn(resp);

        KmaForecastMapper.Slot slot = new KmaForecastMapper.Slot(
            "20250928","1200", SkyStatus.MOSTLY_CLOUDY, PrecipitationType.NONE, 24, 55, 0
        );
        when(kmaAssembler.toSlots(resp.getItems())).thenReturn(List.of(slot));

        Weather snapshot = Weather.builder()
            .location(location)
            .forecastAt(Instant.parse("2025-09-28T03:00:00Z"))
            .forecastedAt(Instant.parse("2025-09-28T02:30:00Z"))
            .skyStatus(SkyStatus.MOSTLY_CLOUDY)
            .type(PrecipitationType.NONE)
            .currentC(24.0)
            .probability(0.0)
            .build();
        when(kmaAssembler.toWeathers(List.of(slot), location)).thenReturn(List.of(snapshot));

        when(weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
            location.getId(), snapshot.getForecastAt(), snapshot.getForecastedAt()
        )).thenReturn(Optional.of(snapshot));

        WeatherSummaryDto dto = new WeatherSummaryDto(
            UUID.randomUUID(), "MOSTLY_CLOUDY",
            new PrecipitationDto("NONE", 0.0, 0.0),
            new TemperatureDto(24.0, 0.0, 0.0, 0.0)
        );
        when(weatherMapper.toFeedWeatherDto(snapshot)).thenReturn(dto);

        // when
        List<WeatherSummaryDto> result = weatherService.getWeather(latitude, longitude);

        // then
        verify(weatherRepository, never()).save(any(Weather.class));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).skyStatus()).isEqualTo("MOSTLY_CLOUDY");
    }
}
