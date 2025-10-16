package com.sprint.otboo.follow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.service.FollowService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FollowController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("팔로워 목록 조회 컨트롤러 테스트")
class FollowersControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void 팔로워_목록_조회_성공_200() throws Exception {
        FollowListItemResponse r1 = new FollowListItemResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            null, null, Instant.parse("2025-10-16T03:00:00Z")
        );
        FollowListItemResponse r2 = new FollowListItemResponse(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            null, null, Instant.parse("2025-10-16T03:00:00Z")
        );

        CursorPageResponse<FollowListItemResponse> resp = new CursorPageResponse<>(
            List.of(r1, r2), null, null, false, 2L, "createdAt", "DESCENDING"
        );

        when(followService.getFollowers(
            Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.anyInt(), Mockito.isNull()))
            .thenReturn(resp);

        mvc.perform(get("/api/follows/followers")
                .param("limit", "2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }
}
