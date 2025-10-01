package com.sprint.otboo.weather.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.service.WeatherLocationQueryService;
import com.sprint.otboo.weather.service.WeatherService;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("WeatherController 테스트")
class WeatherControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WeatherLocationQueryService locationQueryService;

    @MockitoBean
    private WeatherService weatherService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @Test
    @DisplayName("정상 요청은 200과 배열(JSON Array)을 반환한다")
    void 정상_요청은_200과_JSON_배열을_반환해야_한다() throws Exception {
        // given
        when(weatherService.getWeather(37.5665, 126.9780))
            .thenReturn(Collections.emptyList());

        // expect
        mvc.perform(get("/api/weathers")
                .param("longitude", "126.9780")
                .param("latitude", "37.5665"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("파라미터 누락 시 400을 반환한다")
    void 파라미터_누락시_400을_반환해야_한다() throws Exception {
        mvc.perform(get("/api/weathers")
                .param("longitude", "126.9780"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 위경도 형식이면 400을 반환한다")
    void 잘못된_위경도_형식이면_400을_반환해야_한다() throws Exception {
        mvc.perform(get("/api/weathers")
                .param("longitude", "abc")
                .param("latitude", "def"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("위경도 범위를 벗어나면 400과 오류 상세를 반환한다")
    void 위경도_범위_초과시_400과_오류_상세를_반환해야_한다() throws Exception {
        // 위도는 -90~90, 경도는 -180~180 범위 밖
        mvc.perform(get("/api/weathers")
                .param("longitude", "200")
                .param("latitude", "95"))
            .andExpect(status().isBadRequest())
            // ErrorResponse.details 존재(프로토타입 명세 기준)
            .andExpect(jsonPath("$.details.latitude").value("95.0"))
            .andExpect(jsonPath("$.details.longitude").value("200.0"));
    }

    @Test
    @DisplayName("/api/weathers/location 정상 요청은 200을 반환한다")
    void 위치_정보_정상요청은_200을_반환해야_한다() throws Exception {
        // 바디 구조는 구현체에 의존하므로 상태 코드만 검증
        when(locationQueryService.getWeatherLocation(37.5665, 126.9780))
            .thenReturn(null);

        mvc.perform(get("/api/weathers/location")
                .param("longitude", "126.9780")
                .param("latitude", "37.5665"))
            .andExpect(status().isOk());
    }
}
