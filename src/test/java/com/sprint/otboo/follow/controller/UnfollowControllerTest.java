package com.sprint.otboo.follow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.service.FollowService;
import com.sprint.otboo.user.service.UserQueryService;
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
@DisplayName("언팔로우 컨트롤러 테스트")
class UnfollowControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FollowService followService;

    @MockitoBean
    UserQueryService userQueryService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username="11111111-1111-1111-1111-111111111111")
    void 언팔로우_성공_204() throws Exception {
        UUID me       = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID followId = UUID.randomUUID();

        Mockito.doNothing().when(followService).unfollowById(me, followId);

        mvc.perform(delete("/api/follows/{followId}", followId))
            .andExpect(status().isNoContent());

        Mockito.verify(followService).unfollowById(me, followId);
    }

    @Test
    @WithMockUser(username="11111111-1111-1111-1111-111111111111")
    void 언팔로우_대상없음_404() throws Exception {
        UUID me       = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID followId = UUID.randomUUID();

        Mockito.doThrow(new FollowException(ErrorCode.FOLLOW_NOT_FOUND))
            .when(followService).unfollowById(me, followId);

        mvc.perform(delete("/api/follows/{followId}", followId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("FOLLOW_NOT_FOUND"));
    }

    @Test
    void 언팔로우_인증없음_401() throws Exception {
        mvc.perform(delete("/api/follows/{followId}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        Mockito.verifyNoInteractions(followService);
    }
}
