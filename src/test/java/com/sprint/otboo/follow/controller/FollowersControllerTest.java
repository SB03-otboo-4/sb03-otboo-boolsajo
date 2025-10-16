package com.sprint.otboo.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.service.FollowService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = FollowController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = { JwtAuthenticationFilter.class }
        )
    }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("팔로워 목록 조회 컨트롤러 테스트")
class FollowersControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

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

    @Test
    void followers_인증없음_401() throws Exception {
        mvc.perform(get("/api/follows/followers"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verifyNoInteractions(followService);
    }

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void followers_잘못된_cursor_400() throws Exception {
        mvc.perform(get("/api/follows/followers")
                .param("cursor", "nope"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void followers_limit_하한_보정() throws Exception {
        when(followService.getFollowers(any(), any(), any(), eq(1), any()))
            .thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/follows/followers").param("limit", "0"))
            .andExpect(status().isOk());

        verify(followService).getFollowers(any(), any(), any(), eq(1), any());
    }

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void 커서가_빈_문자열이면_검증_통과() throws Exception {
        when(followService.getFollowers(any(), any(), any(), eq(20), any()))
            .thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/follows/followers")
                .param("cursor", ""))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void followers_limit_상한_보정() throws Exception {
        when(followService.getFollowers(any(), any(), any(), eq(100), any()))
            .thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/follows/followers")
                .param("limit", "999"))
            .andExpect(status().isOk());

        verify(followService).getFollowers(any(), any(), any(), eq(100), any());
    }

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void followers_파라미터_전달() throws Exception {
        String cursor = "2025-10-16T03:00:00Z";
        UUID idAfter = UUID.randomUUID();
        String nameLike = "sl";

        when(followService.getFollowers(any(), eq(cursor), eq(idAfter), eq(20), eq(nameLike)))
            .thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/follows/followers")
                .param("cursor", cursor)
                .param("idAfter", idAfter.toString())
                .param("nameLike", nameLike))
            .andExpect(status().isOk());

        verify(followService).getFollowers(any(), eq(cursor), eq(idAfter), eq(20), eq(nameLike));
    }
}
