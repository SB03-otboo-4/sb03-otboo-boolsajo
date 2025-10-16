package com.sprint.otboo.follow.controller;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.service.FollowService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = FollowController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("언팔로우 컨트롤러 테스트")
class UnfollowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FollowService followService;

    @BeforeEach
    void setUp() { SecurityContextHolder.clearContext(); }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private void putAuthWithUserId(UUID userId) {
        List<GrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var token = new UsernamePasswordAuthenticationToken(userId.toString(), null, auths);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    @DisplayName("언팔로우 성공 시 204를 반환한다")
    void 언팔로우_성공_204() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        putAuthWithUserId(followerId);

        willDoNothing().given(followService).unfollow(followerId, followeeId);

        mockMvc.perform(delete("/api/follows/{followeeId}", followeeId))
            .andExpect(status().isNoContent());

        then(followService).should().unfollow(followerId, followeeId);
    }

    @Test
    @DisplayName("팔로우 상태가 아니면 404를 반환한다")
    void 언팔로우_대상이_없으면_404() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        putAuthWithUserId(followerId);

        willAnswer(inv -> { throw new FollowException(ErrorCode.FOLLOW_NOT_FOUND); })
            .given(followService).unfollow(followerId, followeeId);

        mockMvc.perform(delete("/api/follows/{followeeId}", followeeId))
            .andExpect(status().isNotFound());
    }

    @Test
    void 인증_없으면_401() throws Exception {
        SecurityContextHolder.clearContext();

        UUID followeeId = UUID.randomUUID();

        mockMvc.perform(delete("/api/follows/{followeeId}", followeeId))
            .andExpect(status().isUnauthorized());
    }
}
