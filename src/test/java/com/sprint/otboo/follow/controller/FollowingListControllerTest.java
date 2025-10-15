package com.sprint.otboo.follow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.follow.service.FollowService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FollowController.class)
@DisplayName("팔로잉 목록 조회 컨트롤러 테스트")
class FollowingListControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FollowService followService;

    @Test
    @WithMockUser(username = "68e17953-f79f-4d4f-8839-b26054887d5f")
    void 팔로잉_목록_조회_성공_200반환() throws Exception {
        FollowResponse item1 = new FollowResponse(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            new FollowUserSimpleResponse(UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b"), "slinky", null),
            new FollowUserSimpleResponse(UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f"), "buzz",
                "https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );
        FollowResponse item2 = new FollowResponse(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            new FollowUserSimpleResponse(UUID.fromString("e88dc0a5-b1aa-441d-8ea1-540129b1b78b"), "jessie", null),
            new FollowUserSimpleResponse(UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f"), "buzz",
                "https://sprint-sb-project.s3.ap-northeast-2.amazonaws.com/..."),
            Instant.parse("2025-10-14T05:29:40Z")
        );

        CursorPageResponse<FollowResponse> resp = new CursorPageResponse<>(
            List.of(item1, item2),
            null,
            null,
            false,
            2L,
            "createdAt",
            "DESCENDING"
        );

        when(followService.getFollowing(Mockito.any(), Mockito.isNull(), Mockito.anyInt()))
            .thenReturn(resp);

        mvc.perform(get("/api/follows/following")
                .param("size", "2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    void 인증_누락_시_400반환() throws Exception {
        mvc.perform(get("/api/follows/following"))
            .andExpect(status().isUnauthorized());
    }
}
