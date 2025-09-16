package com.sprint.otboo.feed.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import com.sprint.otboo.feed.service.CommentService;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommentController.class)
@ActiveProfiles("test")
@DisplayName("CommentController 테스트")
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Test
    void 댓글을_등록하면_201과_DTO가_반환된다() throws Exception {
        // Given
        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        CommentCreateRequest request = new CommentCreateRequest(feedId, authorId,"첫 댓글");

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
        CommentCreateRequest request = new CommentCreateRequest(feedId,authorId, "첫 댓글");

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
