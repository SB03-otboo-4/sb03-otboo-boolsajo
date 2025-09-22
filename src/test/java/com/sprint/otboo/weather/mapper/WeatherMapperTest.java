package com.sprint.otboo.weather.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("WeatherMapper 테스트")
class WeatherMapperTest {

    private final WeatherMapper mapper = Mappers.getMapper(WeatherMapper.class);

    @Test
    void 전체_주소문자열을_공백으로_분해해야_한다() {
        WeatherLocation wl = WeatherLocation.builder().build();
        wl.setLatitude(new BigDecimal("37.5"));
        wl.setLongitude(new BigDecimal("126.9"));
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");

        WeatherLocationResponse dto = mapper.toLocationResponse(wl);
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 슬래시_구분자_형식도_호환되어야_한다() {
        WeatherLocation wl = WeatherLocation.builder().build();
        wl.setLatitude(new BigDecimal("37.5"));
        wl.setLongitude(new BigDecimal("126.9"));
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시/중구/태평로1가");

        WeatherLocationResponse dto = mapper.toLocationResponse(wl); // ⬅️ 변경
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 중복_공백은_정규화되어야_한다() {
        WeatherLocation wl = WeatherLocation.builder().build();
        wl.setLatitude(new BigDecimal("37.5"));
        wl.setLongitude(new BigDecimal("126.9"));
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시   중구    태평로1가");

        WeatherLocationResponse dto = mapper.toLocationResponse(wl); // ⬅️ 변경
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 구분자만_있는_입력은_빈_리스트() {
        assertThat(WeatherMapper.splitLocationNames(" /  / ")).isEmpty(); // ← 그대로 OK (static)
    }

    @Test
    void 탭과_개행_혼합도_정상_분리() {
        assertThat(WeatherMapper.splitLocationNames("\t서울 / 중구\n태평로1가"))
            .containsExactly("서울","중구","태평로1가");
    }

    @Test
    void toDouble_비null_경로() {
        assertThat(WeatherMapper.toDouble(new BigDecimal("12.3456"))).isEqualTo(12.3456);
    }
}
