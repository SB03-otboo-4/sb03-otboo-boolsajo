package com.sprint.otboo.weather.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherLocationQueryService 테스트")
class WeatherLocationQueryServiceTest {

    private final WeatherLocationQueryService service = new WeatherLocationQueryServiceImpl();

    @Test
    void 저장된_위치가_있으면_DB_저장값으로_응답해야_한다() {
        // given
        double longitude = 126.9780;
        double latitude = 37.5665;

        //when
        WeatherLocationResponse dto = service.getWeatherLocation(longitude, latitude);

        //then
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames())
            .containsExactly("서울특별시", "중구", "태평로1가");
    }

    @Test
    void 저장된_위치가_없으면_계산된_격자와_빈_이름_리스트로_응답해야_한다() {
        //given
        double longitude = 126.9780;
        double latitude = 37.5665;

        //when
        WeatherLocationResponse dto = service.getWeatherLocation(longitude, latitude);

        //then
        assertThat(dto.x()).isEqualTo(60);
        assertThat(dto.y()).isEqualTo(127);
        assertThat(dto.locationNames()).isEmpty();
    }
}
