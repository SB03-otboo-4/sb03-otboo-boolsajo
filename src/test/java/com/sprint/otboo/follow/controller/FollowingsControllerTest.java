package com.sprint.otboo.follow.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
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
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
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
@DisplayName("팔로잉 목록 조회 컨트롤러 테스트")
class FollowingsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FollowService followService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void 팔로잉_목록_조회_성공_200반환() throws Exception {
        FollowListItemResponse item1 = new FollowListItemResponse(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            new UserSummaryResponse(UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b"), "slinky", null),
            new UserSummaryResponse(UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f"), "buzz",
                "https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowListItemResponse item2 = new FollowListItemResponse(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            new UserSummaryResponse(UUID.fromString("e88dc0a5-b1aa-441d-8ea1-540129b1b78b"), "jessie", null),
            new UserSummaryResponse(UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f"), "buzz",
                "https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );

        CursorPageResponse<FollowListItemResponse> resp = new CursorPageResponse<>(
            List.of(item1, item2), null, null, false, 2L, "createdAt", "DESCENDING"
        );

        when(followService.getFollowings(
            Mockito.any(), isNull(), isNull(), anyInt(), isNull()))
            .thenReturn(resp);

        mvc.perform(get("/api/follows/followings")
                .param("followerId", "68e17953-f79f-4d4f-8839-b26054887d5f")
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
    void 잘못된_cursor_400() throws Exception {
        reset(followService);

        mvc.perform(get("/api/follows/followings")
                .param("followerId", "68e17953-f79f-4d4f-8839-b26054887d5f")
                .param("cursor", "not-an-instant")
                .param("limit", "2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(followService);
    }

    @Test
    void limit_하한_보정() throws Exception {
        CursorPageResponse<FollowListItemResponse> resp =
            new CursorPageResponse<>(List.of(), null, null, false, 0L, "createdAt", "DESCENDING");

        UUID follower = UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f");
        when(followService.getFollowings(
            eq(follower), isNull(), isNull(), eq(1), isNull()
        )).thenReturn(resp);

        mvc.perform(get("/api/follows/followings")
                .param("followerId", follower.toString())
                .param("limit", "0"))
            .andExpect(status().isOk());

        verify(followService).getFollowings(eq(follower), isNull(), isNull(), eq(1), isNull());
    }

    @Test
    void limit_상한_보정() throws Exception {
        CursorPageResponse<FollowListItemResponse> resp =
            new CursorPageResponse<>(List.of(), null, null, false, 0L, "createdAt", "DESCENDING");

        UUID follower = UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f");
        when(followService.getFollowings(
            eq(follower), isNull(), isNull(), eq(100), isNull()
        )).thenReturn(resp);

        mvc.perform(get("/api/follows/followings")
                .param("followerId", follower.toString())
                .param("limit", "999"))
            .andExpect(status().isOk());

        verify(followService).getFollowings(eq(follower), isNull(), isNull(), eq(100), isNull());
    }

    @Test
    void followings_파라미터_전달() throws Exception {
        UUID followerId = UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f");
        UUID idAfter = UUID.randomUUID();
        String cursor = "2025-10-14T05:29:40Z";
        String nameLike = "bu";

        when(followService.getFollowings(
            eq(followerId), eq(cursor), eq(idAfter), eq(20), eq(nameLike)
        )).thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/follows/followings")
                .param("followerId", followerId.toString())
                .param("cursor", cursor)
                .param("idAfter", idAfter.toString())
                .param("nameLike", nameLike))
            .andExpect(status().isOk());

        verify(followService).getFollowings(eq(followerId), eq(cursor), eq(idAfter), eq(20), eq(nameLike));
    }

    @Test
    void followings_잘못된_followerId_400() throws Exception {
        mvc.perform(get("/api/follows/followings")
                .param("followerId", "not-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("잘못된 요청 파라미터입니다."));

        verifyNoInteractions(followService);
    }
}
