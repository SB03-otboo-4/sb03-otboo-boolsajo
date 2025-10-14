package com.sprint.otboo.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.service.FollowService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FollowController.class)
@DisplayName("팔로우 요약 정보 API")
class FollowSummaryControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean FollowService followService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean TokenProvider tokenProvider;

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void 팔로우_요약_정보_반환() throws Exception {
        given(followService.getMySummary(any(UUID.class)))
            .willReturn(new FollowSummaryDto(3, 5));

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(3))
            .andExpect(jsonPath("$.followingCount").value(5));
    }
}
