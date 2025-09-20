package com.sprint.otboo.feed.controller;

import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.feed.service.LikeService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LikeController.class)
@ActiveProfiles("test")
@DisplayName("LikeController 테스트")
public class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LikeService likeService;

    @Nested
    @DisplayName("피드 좋아요 등록 테스트")
    class FeedLikeCreateTests {

        @Test
        @DisplayName("좋아요를_등록하면_204를_반환한다")
        void 좋아요를_등록하면_204를_반환한다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            willDoNothing().given(likeService).addLike(feedId, userId);

            // When & Then
            mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
                    .with(csrf())
                    .with(user("tester").roles("USER"))
                    .header("X-USER-ID", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        }
    }
}
