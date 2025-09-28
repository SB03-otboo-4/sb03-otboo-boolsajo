package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.mapper.KmaForecastMapper;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService 테스트")
class WeatherServiceTest {

    @Mock private WeatherLocationQueryService locationQueryService;
    @Mock private KmaShortTermForecastClient forecastClient;
    @Mock private KmaForecastMapper forecastMapper;
    @Mock private WeatherRepository weatherRepository;

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Captor
    private ArgumentCaptor<Weather> weatherCaptor;

    @Test
    void 위경도_입력시_위치조회_예보호출_매핑_업서트_후_타임라인을_반환한다() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;

        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setLatitude(BigDecimal.valueOf(latitude));
        location.setLongitude(BigDecimal.valueOf(longitude));
        location.setX(60);
        location.setY(127);
        location.setLocationNames("서울특별시 중구");

        when(locationQueryService.findOrCreate(latitude, longitude)).thenReturn(location);

        KmaForecastResponse kmaResponse = mock(KmaForecastResponse.class);
        when(forecastClient.getShortTermForecast(location)).thenReturn(kmaResponse);

        Instant forecastAt1 = Instant.parse("2025-09-28T03:00:00Z");
        Instant forecastAt2 = Instant.parse("2025-09-28T06:00:00Z");
        Instant released1 = Instant.parse("2025-09-28T02:30:00Z");
        Instant released2 = Instant.parse("2025-09-28T05:30:00Z");

        Weather w1 = Weather.builder()
            .location(location)
            .forecastAt(forecastAt1)
            .forecastedAt(released1)
            .skyStatus(SkyStatus.CLEAR)
            .asWord(WindStrength.MODERATE)
            .type(PrecipitationType.NONE)
            .currentC(21.0)
            .probability(10.0)
            .build();

        Weather w2 = Weather.builder()
            .location(location)
            .forecastAt(forecastAt2)
            .forecastedAt(released2)
            .skyStatus(SkyStatus.MOSTLY_CLOUDY)
            .asWord(WindStrength.WEAK)
            .type(PrecipitationType.NONE)
            .currentC(23.0)
            .probability(20.0)
            .build();

        when(forecastMapper.toEntities(kmaResponse, location)).thenReturn(Arrays.asList(w1, w2));

        when(weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
            location.getId(), forecastAt1, released1)).thenReturn(Optional.empty());
        when(weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
            location.getId(), forecastAt2, released2)).thenReturn(Optional.empty());

        when(weatherRepository.save(any(Weather.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        List<WeatherSummaryDto> result = weatherService.getWeather(latitude, longitude);

        // then
        verify(locationQueryService).findOrCreate(latitude, longitude);
        verify(forecastClient).getShortTermForecast(location);
        verify(forecastMapper).toEntities(kmaResponse, location);

        // upsert(신규 2건 저장)
        verify(weatherRepository, times(2)).save(weatherCaptor.capture());
        List<Weather> saved = weatherCaptor.getAllValues();
        assertThat(saved).hasSize(2);

        // 반환 타임라인 검증
        assertThat(result).hasSize(2);
        assertThat(result.get(0).forecastAt()).isEqualTo(forecastAt1);
        assertThat(result.get(1).forecastAt()).isEqualTo(forecastAt2);
    }

    @Test
    void 동일_키가_이미_존재하면_저장은_생략하고_진행한다() {
        // given
        Double latitude = 35.1796;
        Double longitude = 129.0756;

        WeatherLocation location = WeatherLocation.builder().build();
        location.setId(UUID.randomUUID());
        location.setLatitude(BigDecimal.valueOf(latitude));
        location.setLongitude(BigDecimal.valueOf(longitude));
        location.setX(98);
        location.setY(76);
        location.setLocationNames("부산광역시 중구");

        when(locationQueryService.findOrCreate(latitude, longitude)).thenReturn(location);

        KmaForecastResponse kmaResponse = mock(KmaForecastResponse.class);
        when(forecastClient.getShortTermForecast(location)).thenReturn(kmaResponse);

        Instant forecastAt = Instant.parse("2025-09-28T09:00:00Z");
        Instant released = Instant.parse("2025-09-28T08:40:00Z");

        Weather w = Weather.builder()
            .location(location)
            .forecastAt(forecastAt)
            .forecastedAt(released)
            .skyStatus(SkyStatus.CLEAR)
            .asWord(WindStrength.MODERATE)
            .type(PrecipitationType.NONE)
            .currentC(24.0)
            .probability(5.0)
            .build();

        when(forecastMapper.toEntities(kmaResponse, location)).thenReturn(List.of(w));

        when(weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
            location.getId(), forecastAt, released)).thenReturn(Optional.of(w));

        // when
        List<WeatherSummaryDto> result = weatherService.getWeather(latitude, longitude);

        // then
        verify(weatherRepository, never()).save(any(Weather.class));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).forecastAt()).isEqualTo(forecastAt);
        assertThat(result.get(0).forecastedAt()).isEqualTo(released);
    }
}
