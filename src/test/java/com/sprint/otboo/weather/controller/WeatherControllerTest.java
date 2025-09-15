package com.sprint.otboo.weather.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WeatherLocationQueryService service;

    @Test
    void 정상_요청은_200과_올바른_본문을_반환해야_한다() throws Exception {
        WeatherLocationResponse response =
            new WeatherLocationResponse(37.5665,126.9780,60,127,
                List.of("서울특별시","중구","태평로1가"));
        given(service.getWeatherLocation(126.9780, 37.5665)).willReturn(response);

        mvc.perform(get("/api/weather/location")
            .param("longitude","126.9780").param("latitude","37.5665"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.x", is(60)))
            .andExpect(jsonPath("$.y", is(127)))
            .andExpect(jsonPath("$.locationNames", hasSize(3)));
    }

    @Test
    void 파라미터가_누락되면_400을_반환해야_한다() throws Exception {
        mvc.perform(get("/api/weather/location").param("longitude","126.9780"))
            .andExpect(status().isBadRequest());
    }
}
