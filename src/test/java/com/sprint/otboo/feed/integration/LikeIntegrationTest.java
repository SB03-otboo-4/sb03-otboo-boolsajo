package com.sprint.otboo.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.repository.FeedLikeRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.fixture.WeatherLocationFixture;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LikeIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;
    @Autowired
    FeedRepository feedRepository;
    @Autowired
    WeatherLocationRepository weatherLocationRepository;
    @Autowired
    WeatherRepository weatherRepository;
    @Autowired
    FeedLikeRepository feedLikeRepository;

    private UUID feedId;
    private UUID userId;
    private User user;
    private Feed feed;

    private CustomUserDetails principal(UUID uid) {
        UserDto dto = new UserDto(uid, Instant.now(), "tester@example.com", "tester",
            Role.USER, LoginType.GENERAL, false);
        return CustomUserDetails.builder().userDto(dto).password("password").build();
    }

    @BeforeEach
    void setUp() {
        // Given
        user = userRepository.save(UserFixture.createUserWithDefault());
        userId = user.getId();

        WeatherLocation location = weatherLocationRepository.save(
            WeatherLocationFixture.createLocationWithDefault());
        Weather weather = weatherRepository.save(WeatherFixture.createWeatherWithDefault(location));

        feed = feedRepository.save(
            Feed.builder()
                .author(user)
                .weather(weather)
                .content("좋아요 테스트용 피드")
                .createdAt(Instant.now())
                .build()
        );
        feedId = feed.getId();
    }

    @Nested
    @DisplayName("좋아요 등록 통합 테스트")
    class LikeCreateIntegrationTests {

        @Test
        void 좋아요를_등록하면_204를_반환하고_DB에_저장된다() throws Exception {
            // Given
            CustomUserDetails principal = principal(userId);

            // When
            mockMvc.perform(
                    post("/api/feeds/{feedId}/like", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                // Then
                .andExpect(status().isNoContent());

            boolean exists = feedLikeRepository.existsByFeedIdAndUserId(feedId, userId);
            assertThat(exists).isTrue();
        }

        @Test
        void 존재하지_않는_피드에_좋아요를_등록하려고_하면_404를_반환한다() throws Exception {
            // Given
            UUID missingFeedId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);

            // When & Then
            mockMvc.perform(
                    post("/api/feeds/{feedId}/like", missingFeedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound());
        }

        @Test
        void 존재하지_않는_유저가_좋아요를_등록하려고_하면_404를_반환한다() throws Exception {
            // Given
            UUID missingUserId = UUID.randomUUID();
            CustomUserDetails principal = principal(missingUserId);

            // When
            mockMvc.perform(
                    post("/api/feeds/{feedId}/like", feedId)
                        .with(csrf())
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                // Then
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("좋아요 삭제 통합 테스트")
    class LikeDeleteIntegrationTests {

        @Test
        void 좋아요를_취소하면_204를_반환하고_DB에서_삭제된다() throws Exception {
            // Given
            CustomUserDetails principal = principal(userId);

            mockMvc.perform(
                    post("/api/feeds/{feedId}/like", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                .andExpect(status().isNoContent());

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}/like", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                // Then
                .andExpect(status().isNoContent());

            boolean exists = feedLikeRepository.existsByFeedIdAndUserId(feedId, userId);
            assertThat(exists).isFalse();
        }

        @Test
        void 존재하지_않는_피드의_좋아요를_취소하려고_하면_404를_반환한다() throws Exception {
            // Given
            UUID missingFeedId = UUID.randomUUID();
            CustomUserDetails principal = principal(userId);

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}/like", missingFeedId)
                        .with(csrf())
                        .with(user(principal))
                )
                // Then
                .andExpect(status().isNotFound());
        }

        @Test
        void 존재하지_않는_유저가_좋아요를_취소하려고_하면_404를_반환한다() throws Exception {
            // Given
            UUID missingUserId = UUID.randomUUID();
            CustomUserDetails principal = principal(missingUserId);

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}/like", feedId)
                        .with(csrf())
                        .with(user(principal))
                )
                // Then
                .andExpect(status().isNotFound());
        }
    }
}
