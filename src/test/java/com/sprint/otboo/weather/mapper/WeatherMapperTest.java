package com.sprint.otboo.weather.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherMapper 테스트")
class WeatherMapperTest {

    @Test
    void 전체_주소문자열을_공백으로_분해해야_한다() {
        WeatherLocation wl = new WeatherLocation();
        wl.setLatitude(37.5);
        wl.setLongitude(126.9);
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시 중구 태평로1가");

        WeatherLocationResponse dto = WeatherMapper.toLocationResponse(wl);
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 슬래시_구분자_형식도_호환되어야_한다() {
        WeatherLocation wl = new WeatherLocation();
        wl.setLatitude(37.5);
        wl.setLongitude(126.9);
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시/중구/태평로1가");

        WeatherLocationResponse dto = WeatherMapper.toLocationResponse(wl);
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }

    @Test
    void 중복_공백은_정규화되어야_한다() {
        WeatherLocation wl = new WeatherLocation();
        wl.setLatitude(37.5);
        wl.setLongitude(126.9);
        wl.setX(60);
        wl.setY(127);
        wl.setLocationNames("서울특별시   중구    태평로1가");

        WeatherLocationResponse dto = WeatherMapper.toLocationResponse(wl);
        assertThat(dto.locationNames()).containsExactly("서울특별시","중구","태평로1가");
    }
}
