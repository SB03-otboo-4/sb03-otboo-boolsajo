package com.sprint.otboo.weather.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.weather.service.WeatherService;
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
    private WeatherService weatherService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @Test
    void 정상_요청은_200과_올바른_본문을_반환해야_한다() throws Exception {
        mvc.perform(get("/api/weathers")
                .param("longitude", "126.9780")
                .param("latitude", "37.5665"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].forecastedAt").exists())
            .andExpect(jsonPath("$[0].forecastAt").exists())
            .andExpect(jsonPath("$[0].location.latitude").exists())
            .andExpect(jsonPath("$[0].location.longitude").exists())
            .andExpect(jsonPath("$[0].skyStatus").isString())
            .andExpect(jsonPath("$[0].precipitation.type").isString())
            .andExpect(jsonPath("$[0].precipitation.probability").isNumber())
            .andExpect(jsonPath("$[0].temperature.current").isNumber())
            .andExpect(jsonPath("$[0].humidity.current").isNumber());
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
