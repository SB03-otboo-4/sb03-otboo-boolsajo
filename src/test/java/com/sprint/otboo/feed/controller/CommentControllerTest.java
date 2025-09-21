package com.sprint.otboo.feed.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import com.sprint.otboo.feed.service.CommentService;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommentController.class)
@ActiveProfiles("test")
@DisplayName("CommentController 테스트")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    private CommentService commentService;

    @Nested
    @DisplayName("댓글 등록 테스트")
    class CommentCreateTests {

        @Test
        void 댓글을_등록하면_201과_DTO가_반환된다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();

            CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "첫 댓글");

            CommentDto response = new CommentDto(
                UUID.randomUUID(),
                Instant.now(),
                feedId,
                new AuthorDto(authorId, "홍길동", "https://example.com/profile.png"),
                "첫 댓글"
            );

            given(commentService.create(any(UUID.class), any(UUID.class), anyString()))
                .willReturn(response);

            // When & Then
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").value("첫 댓글"))
                .andExpect(jsonPath("$.feedId").value(feedId.toString()))
                .andExpect(jsonPath("$.author.name").value("홍길동"));
        }

        @Test
        @DisplayName("존재하지_않는_피드에_댓글을_등록하면_404를_반환한다")
        void 존재하지_않는_피드에_댓글을_등록하면_404를_반환한다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "첫 댓글");

            given(commentService.create(any(UUID.class), any(UUID.class), anyString()))
                .willThrow(new FeedNotFoundException());

            // When & Then
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("존재하지_않는_작성자로_댓글을_등록하면_404를_반환한다")
        void 존재하지_않는_작성자로_댓글을_등록하면_404를_반환한다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "첫 댓글");

            given(commentService.create(any(UUID.class), any(UUID.class), anyString()))
                .willThrow(new UserNotFoundException());

            // When & Then
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("내용이_비어있으면_400을_반환한다")
        void 내용이_비어있으면_400을_반환한다() throws Exception {
            // Given: 공백/빈 문자열
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            CommentCreateRequest badRequest = new CommentCreateRequest(feedId, authorId, "   ");

            // When & Then
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(badRequest))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("댓글 조회 테스트")
    class CommentReadTests {

        @Test
        void 댓글을_조회하면_200과_DTO페이지가_반환된다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            Instant now = Instant.now();

            CommentDto d1 = new CommentDto(
                UUID.randomUUID(),
                now,
                feedId,
                new AuthorDto(authorId, "홍길동", "https://example.com/p1.png"),
                "첫 댓글"
            );
            CommentDto d2 = new CommentDto(
                UUID.randomUUID(),
                now.minusSeconds(10),
                feedId,
                new AuthorDto(authorId, "홍길동", "https://example.com/p1.png"),
                "둘째 댓글"
            );

            CursorPageResponse<CommentDto> page = new CursorPageResponse<>(
                List.of(d1, d2),
                now.toString(),
                d2.id().toString(),
                true,
                2L,
                "createAt",
                "DESCENDING"
            );

            given(commentService.getComments(any(UUID.class), any(String.class), any(UUID.class),
                anyInt()))
                .willReturn(page);

            // When & Then
            mockMvc.perform(
                    get(
                        "/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .param("limit", "2")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$.content[0].feedId").value(feedId.toString()))
                .andExpect(jsonPath("$.content[0].author.name").value("홍길동"));
        }

        @Test
        void limit이_없으면_400을_반환한다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();

            // When & Then
            mockMvc.perform(
                    get(
                        "/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 존재하지_않는_feedId면_404를_반환한다() throws Exception {
            // Given
            UUID feedId = UUID.randomUUID();

            given(commentService.getComments(any(UUID.class), any(String.class), any(UUID.class),
                anyInt()))
                .willThrow(new FeedNotFoundException());

            // When & Then
            mockMvc.perform(
                    get(
                        "/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .param("limit", "10")
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }
}
