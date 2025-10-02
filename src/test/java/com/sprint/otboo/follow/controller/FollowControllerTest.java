package com.sprint.otboo.follow.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sprint.otboo.follow.controller.FollowController;
import com.sprint.otboo.follow.dto.FollowDto;
import com.sprint.otboo.follow.service.FollowService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("팔로우 생성 API 테스트")
@WebMvcTest(FollowController.class)
class FollowControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FollowService followService;

    @Test
    @DisplayName("팔로우를 생성하면 201과 응답을 반환해야 한다")
    void 팔로우를_생성하면_201과_응답을_반환해야_한다() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();

        when(followService.create(followerId, followeeId))
            .thenReturn(new FollowDto(followId, followerId, followeeId));

        String body = """
          {"followerId":"%s","followeeId":"%s"}
        """.formatted(followerId, followeeId);

        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(followId.toString()))
            .andExpect(jsonPath("$.followerId").value(followerId.toString()))
            .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));
    }

    @Test
    @DisplayName("자기 자신을 팔로우하면 400을 반환해야 한다")
    void 자기_자신을_팔로우하면_400을_반환해야_한다() throws Exception {
        UUID id = UUID.randomUUID();
        String body = """
          {"followerId":"%s","followeeId":"%s"}
        """.formatted(id, id);

        doThrow(new IllegalArgumentException("Cannot follow self"))
            .when(followService).create(id, id);

        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 팔로우 중이면 409를 반환해야 한다")
    void 이미_팔로우_중이면_409를_반환해야_한다() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        String body = """
          {"followerId":"%s","followeeId":"%s"}
        """.formatted(followerId, followeeId);

        doThrow(new IllegalStateException("Already following"))
            .when(followService).create(followerId, followeeId);

        mockMvc.perform(post("/api/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }
}
