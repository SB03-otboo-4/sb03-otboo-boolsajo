package com.sprint.otboo.follow.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.service.FollowService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FollowController.class)
@DisplayName("언팔로우 컨트롤러 테스트")
class UnfollowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FollowService followService;

    // requireUserIdFromSecurityContext()를 그대로 쓰되, 스파이로 followerId 주입
    @Spy
    FollowController followController;

    @Test
    @WithMockUser
    @DisplayName("언팔로우 성공 시 204를 반환한다")
    void 언팔로우_성공_204() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        doReturn(followerId).when(followController).requireUserIdFromSecurityContext();

        mockMvc.perform(delete("/api/follows/{followeeId}", followeeId))
            .andExpect(status().isNoContent());

        verify(followService).unfollow(followerId, followeeId);
    }

    @Test
    @WithMockUser
    @DisplayName("팔로우 상태가 아니면 404를 반환한다")
    void 언팔로우_대상이_없으면_404() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();

        doReturn(followerId).when(followController).requireUserIdFromSecurityContext();
        given(followService.unfollow(followerId, followeeId))
            .willAnswer(invocation -> { throw new FollowException(ErrorCode.NOT_FOLLOWING); });

        mockMvc.perform(delete("/api/follows/{followeeId}", followeeId))
            .andExpect(status().isNotFound());
    }
}
