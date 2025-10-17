package com.sprint.otboo.follow.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.service.FollowService;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.service.UserQueryService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

@DisplayName("팔로우 생성 API 테스트")
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
class FollowControllerTest {

    private static final String AUTH_USER = "11111111-1111-1111-1111-111111111111";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FollowService followService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    UserQueryService userQueryService;

    @Test
    @WithMockUser(username = AUTH_USER)
    void 팔로우를_생성하면_200과_프로토타입_응답을_반환한다() throws Exception {
        UUID followerId = UUID.fromString(AUTH_USER);
        UUID followeeId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();

        // service.create → 기존대로 FollowDto 반환
        when(followService.create(followerId, followeeId))
            .thenReturn(new FollowDto(followId, followerId, followeeId));

        // ✅ followee/follower 요약 스텁
        when(userQueryService.getSummary(followerId))
            .thenReturn(new UserSummaryResponse(followerId, "me", null));
        when(userQueryService.getSummary(followeeId))
            .thenReturn(new UserSummaryResponse(followeeId, "target", "https://example.com/p.png"));

        String body = "{ \"followeeId\": \"" + followeeId + "\" }";

        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(followId.toString()))
            .andExpect(jsonPath("$.follower.userId").value(followerId.toString()))
            .andExpect(jsonPath("$.follower.name").value("me"))
            .andExpect(jsonPath("$.followee.userId").value(followeeId.toString()))
            .andExpect(jsonPath("$.followee.name").value("target"));
    }

    @Test
    @WithMockUser(username = AUTH_USER)
    void 자기_자신을_팔로우하면_400과_에러바디를_반환해야_한다() throws Exception {
        UUID followerId = UUID.fromString(AUTH_USER);
        UUID followeeId = followerId;

        doThrow(new FollowException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED))
            .when(followService).create(followerId, followeeId);

        String body = "{ \"followeeId\": \"" + followeeId + "\" }";

        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("FOLLOW_SELF_NOT_ALLOWED"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @WithMockUser(username = AUTH_USER)
    void 이미_팔로우_중이면_409와_에러바디를_반환해야_한다() throws Exception {
        UUID followerId = UUID.fromString(AUTH_USER);
        UUID followeeId = UUID.randomUUID();

        doThrow(new FollowException(ErrorCode.FOLLOW_ALREADY_EXISTS))
            .when(followService).create(followerId, followeeId);

        String body = "{ \"followeeId\": \"" + followeeId + "\" }";

        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("FOLLOW_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
