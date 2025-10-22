package com.sprint.otboo.feed.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.paging.InvalidPagingParamException;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.service.FeedService;
import com.sprint.otboo.fixture.FeedFixture;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeedController.class)
@ActiveProfiles("test")
class FeedControllerReadTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TokenProvider tokenProvider;
    @MockitoBean
    JwtRegistry jwtRegistry;
    @MockitoBean
    FeedService feedService;

    @Test
    void 피드를_조회하면_200과_DTO가_반환된다() throws Exception {
        // given
        FeedDto dto1 = FeedFixture.createDtoWithDefault();
        FeedDto dto2 = FeedFixture.createDtoWithDefault();
        CursorPageResponse<FeedDto> resp = new CursorPageResponse<>(
            List.of(dto1, dto2), null, null,
            false, 2L, "createdAt", "DESCENDING"
        );

        given(feedService.getFeeds(any(), any(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any()))
            .willReturn(resp);

        // when & then
        mockMvc.perform(
                get("/api/feeds")
                    .param("limit", "3")
                    .with(user("tester").roles("USER"))
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data", hasSize(2)))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.data[0].id").isNotEmpty())
            .andExpect(jsonPath("$.data[0].author.name").isNotEmpty())
            .andExpect(jsonPath("$.data[0].weather.skyStatus").isNotEmpty())
            .andExpect(jsonPath("$.data[0].likeCount").isNumber());
    }

    @Test
    void createdAt_커서로_피드를_조회하면_내림차순으로_정렬되고_커서를_반환한다() throws Exception {
        // given
        Instant base = Instant.now();
        FeedDto dto1 = FeedFixture.createDtoWithCreatedAt(base);
        FeedDto dto2 = FeedFixture.createDtoWithCreatedAt(base.plusMillis(1));
        FeedDto dto3 = FeedFixture.createDtoWithCreatedAt(base.plusMillis(2));

        CursorPageResponse<FeedDto> resp = new CursorPageResponse<>(
            List.of(dto3, dto2, dto1),
            dto1.createdAt().toString(),
            dto1.id().toString(),
            true,
            3L,
            "createdAt",
            "DESCENDING"
        );

        given(feedService.getFeeds(
            any(), any(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any()
        )).willReturn(resp);

        // when & then
        mockMvc.perform(
                get("/api/feeds")
                    .with(user("tester").roles("USER"))
                    .param("limit", "3")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data[0].id").value(dto3.id().toString()))
            .andExpect(jsonPath("$.data[0].createdAt").value(dto3.createdAt().toString()))
            .andExpect(jsonPath("$.data[1].id").value(dto2.id().toString()))
            .andExpect(jsonPath("$.data[1].createdAt").value(dto2.createdAt().toString()))
            .andExpect(jsonPath("$.data[2].id").value(dto1.id().toString()))
            .andExpect(jsonPath("$.data[2].createdAt").value(dto1.createdAt().toString()))
            .andExpect(jsonPath("$.nextCursor").value(dto1.createdAt().toString()))
            .andExpect(jsonPath("$.nextIdAfter").value(dto1.id().toString()))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    void likeCount_커서로_피드를_조회하면_내림차순으로_정렬되고_커서를_반환한다() throws Exception {
        // given
        FeedDto dto1 = FeedFixture.createDtoWithLikeCount(1L);
        FeedDto dto2 = FeedFixture.createDtoWithLikeCount(2L);
        FeedDto dto3 = FeedFixture.createDtoWithLikeCount(3L);

        CursorPageResponse<FeedDto> resp = new CursorPageResponse<>(
            List.of(dto3, dto2, dto1),
            String.valueOf(dto1.likeCount()),
            dto1.id().toString(),
            true,
            4L,
            "likeCount",
            "DESCENDING"
        );

        given(feedService.getFeeds(any(), any(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any()))
            .willReturn(resp);

        // when & then
        mockMvc.perform(
                get("/api/feeds")
                    .with(user("tester").roles("USER"))
                    .param("limit", "3")
                    .param("sortBy", "likeCount")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data[0].likeCount").value(3))
            .andExpect(jsonPath("$.data[1].likeCount").value(2))
            .andExpect(jsonPath("$.data[2].likeCount").value(1))
            .andExpect(jsonPath("$.nextCursor").value(String.valueOf(dto1.likeCount())))
            .andExpect(jsonPath("$.nextIdAfter").value(dto1.id().toString()))
            .andExpect(jsonPath("$.sortBy").value("likeCount"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"))
            .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void limit가_0이면_400을_반환한다() throws Exception {
        // given
        given(feedService.getFeeds(any(), any(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any()))
            .willThrow(new InvalidPagingParamException(ErrorCode.INVALID_PAGING_LIMIT));

        // when & then
        mockMvc.perform(
                get("/api/feeds")
                    .with(user("tester").roles("USER"))
                    .param("limit", "0")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void sortBy가_지원되지_않으면_400을_반환한다() throws Exception {
        // given
        given(feedService.getFeeds(any(), any(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any()))
            .willThrow(new InvalidPagingParamException(ErrorCode.INVALID_SORT_BY));

        // when & then
        mockMvc.perform(
                get("/api/feeds")
                    .with(user("tester").roles("USER"))
                    .param("limit", "10")
                    .param("sortBy", "invalidField")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void sortDirection이_지원되지_않으면_400을_반환한다() throws Exception {
        // given
        given(feedService.getFeeds(any(), any(), anyInt(), anyString(), anyString(),
            any(), any(), any(), any()))
            .willThrow(new InvalidPagingParamException(ErrorCode.INVALID_SORT_DIRECTION));

        // when & then
        mockMvc.perform(
                get("/api/feeds")
                    .with(user("tester").roles("USER"))
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DOWNWARD")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
