package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("WeatherLocationQueryService 테스트")
class WeatherLocationQueryServiceTest {

    private WeatherLocationRepository repo;
    private WeatherLocationQueryService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(WeatherLocationRepository.class);
        service = new WeatherLocationQueryServiceImpl(repo);
    }

    @Test
    void 저장된_위치가_있으면_DB_저장값으로_응답해야_한다() {
        // given
        WeatherLocation wl = new WeatherLocation();
        wl.setLatitude(37.5665);
        wl.setLongitude(126.9780);
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");
        given(repo.findFirstByLatitudeAndLongitude(37.5665, 126.9780))
            .willReturn(Optional.of(wl));

        // when
        WeatherLocationResponse dto = service.getWeatherLocation(126.9780, 37.5665);

        // then
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames()).containsExactly("서울특별시", "중구", "태평로1가");
    }

    @Test
    void 저장된_위치가_없으면_계산된_격자와_빈_이름_리스트로_응답해야_한다() {
        // given
        given(repo.findFirstByLatitudeAndLongitude(37.5665, 126.9780))
            .willReturn(Optional.empty());

        // when
        WeatherLocationResponse dto = service.getWeatherLocation(126.9780, 37.5665);

        // then
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames()).isEmpty();
    }
}
