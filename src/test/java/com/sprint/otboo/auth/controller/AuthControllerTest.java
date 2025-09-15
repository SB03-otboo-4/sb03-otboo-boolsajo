package com.sprint.otboo.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.common.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void csrf토큰_조회_성공() throws Exception {
        var result = mockMvc.perform(get("/api/auth/csrf-token"))
            .andExpect(status().isNoContent())
            .andReturn();

        var setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        String xsrf = setCookies.stream()
            .filter(v -> v.startsWith("XSRF-TOKEN="))
            .findFirst().orElse(null);
        assertThat(xsrf).as("XSRF-TOKEN Set-Cookie").isNotNull();
        assertThat(xsrf).doesNotContain("HttpOnly");
    }

    @Test
    void CSRF_없이_POST_요청하면_403_반환한다() throws Exception {
        mockMvc.perform(post("/no-mapping"))
            .andExpect(status().isForbidden());
    }

}
