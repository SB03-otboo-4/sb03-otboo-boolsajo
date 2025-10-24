package com.sprint.otboo.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.notification.dto.request.NotificationQueryParams;
import com.sprint.otboo.notification.dto.response.NotificationCursorResponse;
import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.NotificationLevel;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(NotificationController.class)
@DisplayName("NotificationController 테스트")
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    private CustomUserDetails userPrincipal(UUID id) {
        UserDto dto = new UserDto(
            id,
            Instant.now(),
            "test@test.com",
            "testUser",
            Role.USER,
            LoginType.GENERAL,
            false
        );
        return CustomUserDetails.builder()
            .userDto(dto)
            .password("encodedPassword")
            .build();
    }

    private NotificationDto notificationDto(UUID receiverId) {
        return new NotificationDto(
            UUID.randomUUID(),
            Instant.now(),
            receiverId,
            "테스트 알림",
            "테스트 알림입니다.",
            NotificationLevel.INFO
        );
    }
    @Test
    void 인증_사용자가_알림_목록을_조회하여_커서_기반_응답_반환() throws Exception {
        // given
        UUID receiverId = UUID.randomUUID();
        CustomUserDetails principal = userPrincipal(receiverId);

        NotificationDto dto = notificationDto(receiverId);
        NotificationCursorResponse response = NotificationCursorResponse.from(
            List.of(dto),
            "2025-09-24T10:00:00Z",
            UUID.randomUUID().toString(),
            true,
            42L,
            "createdAt",
            "DESCENDING"
        );
        given(notificationService.getNotifications(any(), any()))
            .willReturn(response);

        // when
        mockMvc.perform(get("/api/notifications")
            .param("limit","10")
            .with(user(principal)))
            // then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(dto.id().toString()))
            .andExpect(jsonPath("$.data[0].level").value(dto.level().name()))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.sortBy").value("createdAt"));

        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<NotificationQueryParams> paramCaptor = ArgumentCaptor.forClass(NotificationQueryParams.class);

        then(notificationService).should().getNotifications(idCaptor.capture(), paramCaptor.capture());

        assertThat(idCaptor.getValue()).isEqualTo(receiverId);
        assertThat(paramCaptor.getValue().limit()).isEqualTo(10);
    }

    @Test
    void 인증_사용자가_알림_읽음_처리_시_204_반환() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();
        CustomUserDetails principal = userPrincipal(UUID.randomUUID());

        // when
        ResultActions result = mockMvc.perform(delete("/api/notifications/{notificationId}",notificationId)
            .with(user(principal))
            .with(csrf()));

        // then
        result.andExpect(status().isNoContent());
        then(notificationService).should().deleteNotification(notificationId);
    }
}
