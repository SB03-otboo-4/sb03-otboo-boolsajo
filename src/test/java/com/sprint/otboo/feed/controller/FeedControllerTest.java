package com.sprint.otboo.feed.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.common.exception.feed.FeedAccessDeniedException;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import com.sprint.otboo.feed.service.FeedService;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FeedController.class)
@ActiveProfiles("test")
class FeedControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TokenProvider tokenProvider;
    @MockitoBean
    JwtRegistry jwtRegistry;
    @MockitoBean
    FeedService feedService;

    private CustomUserDetails principal(UUID id) {
        UserDto userDto = new UserDto(id, Instant.now(), "tester@example.com", "tester", Role.USER,
            LoginType.GENERAL, false);
        return CustomUserDetails.builder().userDto(userDto).password("password").build();
    }

    private FeedDto sampleFeedDto(UUID feedId, UUID authorId, UUID weatherId, UUID clothesId,
        String content) {
        AuthorDto author = new AuthorDto(authorId, "홍길동", "https://example.com/profile.png");
        TemperatureDto temp = new TemperatureDto(21.0, -1.0, 18.0, 25.0);
        PrecipitationDto pre = new PrecipitationDto("RAIN", 12.3, 80.0);
        WeatherSummaryDto weather = new WeatherSummaryDto(weatherId, "CLOUDY", pre, temp);
        OotdDto ootd = new OotdDto(clothesId, "후드티", "https://example.com/image.png",
            ClothesType.TOP, List.of());
        return new FeedDto(feedId, Instant.now(), Instant.now(), author, weather, List.of(ootd),
            content, 5L, 3, true);
    }

    @Nested
    class FeedCreateTests {

        @Test
        void 피드를_등록하면_201과_DTO가_반환한다() throws Exception {
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            FeedCreateRequest request = new FeedCreateRequest(authorId, weatherId,
                List.of(clothesId), "오늘의 코디");

            FeedDto dto = sampleFeedDto(UUID.randomUUID(), authorId, weatherId, clothesId,
                "오늘의 코디");
            given(feedService.create(any(FeedCreateRequest.class))).willReturn(dto);

            mockMvc.perform(
                    post("/api/feeds")
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("오늘의 코디"))
                .andExpect(jsonPath("$.author.name").value("홍길동"))
                .andExpect(jsonPath("$.weather.skyStatus").value("CLOUDY"))
                .andExpect(jsonPath("$.ootds[0].name").value("후드티"))
                .andExpect(jsonPath("$.likeCount").value(5))
                .andExpect(jsonPath("$.commentCount").value(3));
        }

        @Test
        void 존재하지_않는_작성자로_피드를_등록하려하면_404를_반환한다() throws Exception {
            FeedCreateRequest request = new FeedCreateRequest(UUID.randomUUID(), UUID.randomUUID(),
                List.of(UUID.randomUUID()), "오늘의 코디");
            given(feedService.create(any(FeedCreateRequest.class))).willThrow(
                new UserNotFoundException());

            mockMvc.perform(
                    post("/api/feeds")
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 작성자를_입력하지_않으면_400을_반환한다() throws Exception {
            FeedCreateRequest request = new FeedCreateRequest(null, UUID.randomUUID(),
                List.of(UUID.randomUUID()), "오늘의 코디");

            mockMvc.perform(
                    post("/api/feeds")
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class FeedUpdateTests {

        @Test
        void 피드를_수정하면_200과_DTO가_반환된다() throws Exception {
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);
            FeedUpdateRequest request = new FeedUpdateRequest("오늘의 코디(수정)");

            FeedDto dto = sampleFeedDto(feedId, userId, UUID.randomUUID(), UUID.randomUUID(),
                "오늘의 코디(수정)");
            given(feedService.update(any(), any(), any())).willReturn(dto);

            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedId.toString()))
                .andExpect(jsonPath("$.content").value("오늘의 코디(수정)"))
                .andExpect(jsonPath("$.author.name").value("홍길동"));
        }

        @Test
        void 존재하지_않는_피드를_수정하면_404를_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            CustomUserDetails principal = principal(UUID.randomUUID());
            FeedUpdateRequest request = new FeedUpdateRequest("수정");
            given(feedService.update(any(), any(), any())).willThrow(new FeedNotFoundException());

            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 해당_피드의_작성자가_아니면_403을_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            CustomUserDetails principal = principal(UUID.randomUUID());
            FeedUpdateRequest request = new FeedUpdateRequest("수정");
            given(feedService.update(any(), any(), any())).willThrow(
                new FeedAccessDeniedException());

            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void content가_null이거나_blank면_400을_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            CustomUserDetails principal = principal(UUID.randomUUID());

            FeedUpdateRequest requestNull = new FeedUpdateRequest(null);
            FeedUpdateRequest requestBlank = new FeedUpdateRequest("   ");

            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requestNull))
                )
                .andExpect(status().isBadRequest());

            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requestBlank))
                )
                .andExpect(status().isBadRequest());
        }

        @Test
        void 존재하지_않는_작성자로_수정시도하면_404를_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            CustomUserDetails principal = principal(UUID.randomUUID());
            FeedUpdateRequest request = new FeedUpdateRequest("수정");
            given(feedService.update(any(), any(), any())).willThrow(new UserNotFoundException());

            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    class FeedDeleteTests {

        @Test
        void 본인_피드를_삭제하면_204를_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);
            willDoNothing().given(feedService).delete(userId, feedId);

            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                .andExpect(status().isNoContent());
        }

        @Test
        void 존재하지_않는_피드를_삭제하면_404를_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);

            willThrow(new FeedNotFoundException()).given(feedService).delete(userId, feedId);

            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 해당_피드의_작성자가_아니면_403을_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);

            willThrow(new FeedAccessDeniedException()).given(feedService).delete(userId, feedId);

            // When & Then
            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 존재하지_않는_작성자로_삭제하면_404를_반환한다() throws Exception {
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);

            willThrow(new UserNotFoundException()).given(feedService).delete(userId, feedId);

            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }
}
