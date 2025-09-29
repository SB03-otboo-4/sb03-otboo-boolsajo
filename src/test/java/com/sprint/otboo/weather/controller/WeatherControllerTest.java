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
    private WeatherLocationQueryService service;

    @MockitoBean
    private WeatherService weatherService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @Test
    void 정상_요청은_200과_요약_본문을_반환해야_한다() throws Exception {
        WeatherSummaryDto dto = new WeatherSummaryDto(
            UUID.randomUUID(),
            "CLEAR",
            new PrecipitationDto("NONE", 0.0, 10.0),
            new TemperatureDto(22.0, 0.0, 0.0, 0.0)
        );
        when(weatherService.getWeather(37.5665, 126.9780)).thenReturn(List.of(dto));

        mvc.perform(get("/api/weathers")
                .param("longitude", "126.9780")
                .param("latitude", "37.5665"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].weatherId").exists())
            .andExpect(jsonPath("$[0].skyStatus").value("CLEAR"))
            .andExpect(jsonPath("$[0].precipitation.type").value("NONE"))
            .andExpect(jsonPath("$[0].precipitation.probability").value(10.0))
            .andExpect(jsonPath("$[0].temperature.current").value(22.0));
    }

    @Test
    void 파라미터_누락시_400을_반환해야_한다() throws Exception {
        mvc.perform(get("/api/weathers")
                .param("longitude", "126.9780"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void 잘못된_위경도_형식이면_400을_반환해야_한다() throws Exception {
        mvc.perform(get("/api/weathers")
                .param("longitude", "abc")
                .param("latitude", "def"))
            .andExpect(status().isBadRequest());
    }
}
