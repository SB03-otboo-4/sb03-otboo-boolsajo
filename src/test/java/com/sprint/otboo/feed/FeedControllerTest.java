package com.sprint.otboo.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.feed.controller.FeedController;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.service.FeedService;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FeedController.class)
@ActiveProfiles("test")
@DisplayName("FeedController 테스트")
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedService feedService;

    @Test
    void 피드를_등록하면_201과_DTO가_반환한다() throws Exception {
        // Given
        UUID authorId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();

        FeedCreateRequest request = new FeedCreateRequest(
            authorId,
            weatherId,
            List.of(clothesId),
            "오늘의 코디"
        );
        FeedDto.Author author = new FeedDto.Author(
            UUID.randomUUID(),
            "홍길동",
            "https://example.com/profile.png"
        );

        FeedDto.Weather.Temperature temperature =
            new FeedDto.Weather.Temperature(20.5, -1.0, 18.0, 25.0);

        FeedDto.Weather.Precipitation precipitation =
            new FeedDto.Weather.Precipitation("RAIN", 12.3, 80.0);

        FeedDto.Weather weather = new FeedDto.Weather(
            UUID.randomUUID(),
            "CLOUDY",
            precipitation,
            temperature
        );

        FeedDto.OotdItem ootd = new FeedDto.OotdItem(
            UUID.randomUUID(),
            "스포츠 자켓"
        );

        FeedDto response = new FeedDto(
            UUID.randomUUID(),
            Instant.now(),
            Instant.now(),
            author,
            weather,
            List.of(ootd),
            "오늘의 코디",
            0L,
            0,
            false
        );

        given(feedService.create(any(FeedCreateRequest.class))).willReturn(response);

        // When & Then
        mockMvc.perform(
                post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").value("오늘의 코디"))
            .andExpect(jsonPath("$.author.name").value("홍길동"))
            .andExpect(jsonPath("$.weather.skyStatus").value("CLOUDY"))
            .andExpect(jsonPath("$.ootds[0].name").value("스포츠 자켓"));
    }

    @Test
    void 존재하지_않는_작성자로_피드를_등록하려하면_404를_반환한다() throws Exception {
        // Given
        FeedCreateRequest request = new FeedCreateRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            "오늘의 코디"
        );

        given(feedService.create(any(FeedCreateRequest.class)))
            .willThrow(new EntityNotFoundException("User not found"));

        // When & Then
        mockMvc.perform(
                post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(request))
            )
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void 작성자를_입력하지_않으면_400을_반환한다() throws Exception {
        // Given
        FeedCreateRequest badRequest = new FeedCreateRequest(
            null,
            UUID.randomUUID(),
            List.of(UUID.randomUUID()),
            "오늘의 코디"
        );

        // When & Then
        mockMvc.perform(
                post("/api/feeds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(badRequest))
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
