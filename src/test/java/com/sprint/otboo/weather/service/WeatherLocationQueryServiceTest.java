package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WeatherLocationQueryServiceTest {

    private final WeatherLocationRepository repo = Mockito.mock(WeatherLocationRepository.class);
    private final LocationNameResolver resolver = Mockito.mock(LocationNameResolver.class);
    private final WeatherLocationQueryService service = new WeatherLocationQueryService(repo, resolver);

    @Test
    void 저장된_위치가_있으면_DB_저장값으로_응답해야_한다() {
        WeatherLocation wl = new WeatherLocation();
        wl.setId(UUID.randomUUID());
        wl.setLatitude(37.5665);
        wl.setLongitude(126.9780);
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");

        given(repo.findByXAndY(60, 127)).willReturn(Optional.of(wl));

        WeatherLocationResponse dto = service.getWeatherLocation(126.9780, 37.5665);
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 저장된_위치가_없으면_계산된_격자와_빈_이름_리스트로_응답해야_한다() {
        given(repo.findByXAndY(60, 127)).willReturn(Optional.empty());
        given(resolver.resolve(37.5665, 126.9780)).willReturn(List.of());

        WeatherLocationResponse dto = service.getWeatherLocation(126.9780, 37.5665);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames()).isEmpty();
    }
}
