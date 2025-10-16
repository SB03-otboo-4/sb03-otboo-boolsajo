package com.sprint.otboo.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.fixture.ClothesFixture;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    FeedRepository feedRepository;
    @Autowired
    WeatherLocationRepository weatherLocationRepository;
    @Autowired
    WeatherRepository weatherRepository;
    @Autowired
    ClothesRepository clothesRepository;

    private User author;
    private UUID authorId;
    private Weather weather;
    private UUID clothesId;

    private CustomUserDetails principal(UUID uid) {
        UserDto dto = new UserDto(
            uid, Instant.now(), "tester@example.com", "tester",
            Role.USER, LoginType.GENERAL, false
        );
        return CustomUserDetails.builder().userDto(dto).password("password").build();
    }

    @BeforeEach
    void setUp() {
        author = userRepository.save(UserFixture.createUserWithDefault());
        authorId = author.getId();
        WeatherLocation location = weatherLocationRepository.save(
            WeatherLocationFixture.createLocationWithDefault());
        weather = weatherRepository.save(WeatherFixture.createWeatherWithDefault(location));
        Clothes clothes = ClothesFixture.create(author, "테스트 의상", "https://example.com/img.png",
            ClothesType.TOP);
        clothesId = clothesRepository.save(clothes).getId();
    }

    @Nested
    @DisplayName("피드 등록 통합 테스트")
    class FeedCreateIntegrationTests {

        @Test
        void 피드를_등록하면_201과_DTO가_반환되고_DB에_저장된다() throws Exception {
            // Given
            FeedCreateRequest request = new FeedCreateRequest(
                authorId,
                weather.getId(),
                List.of(clothesId),
                "오늘의 코디 - 통합테스트"
            );

            // When
            MvcResult result = mockMvc.perform(
                    post("/api/feeds")
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("오늘의 코디 - 통합테스트"))
                .andExpect(jsonPath("$.author.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.weather.weatherId").value(weather.getId().toString()))
                .andReturn();

            // Then
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            String createdId = JsonPath.read(body, "$.id");
            Optional<Feed> saved = feedRepository.findById(UUID.fromString(createdId));
            assertThat(saved).isPresent();
            assertThat(saved.get().getAuthor().getId()).isEqualTo(authorId);
            assertThat(saved.get().getWeather().getId()).isEqualTo(weather.getId());
            assertThat(saved.get().getContent()).isEqualTo("오늘의 코디 - 통합테스트");
        }

        @Test
        void 존재하지_않는_작성자로_등록하면_404가_반환된다() throws Exception {
            // Given
            UUID missingAuthorId = UUID.randomUUID();

            FeedCreateRequest request = new FeedCreateRequest(
                missingAuthorId,
                weather.getId(),
                List.of(clothesId),
                "오늘의 코디"
            );

            // When
            mockMvc.perform(
                    post("/api/feeds")
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isNotFound());
        }

        @Test
        void 작성자가_null이면_400이_반환된다() throws Exception {
            // Given
            FeedCreateRequest request = new FeedCreateRequest(
                null,
                weather.getId(),
                List.of(clothesId),
                "오늘의 코디"
            );

            // When
            mockMvc.perform(
                    post("/api/feeds")
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("피드 수정 통합 테스트")
    class FeedUpdateIntegrationTests {

        @Test
        void 작성자가_피드를_수정하면_200과_내용이_변경된다() throws Exception {
            // Given
            Feed feed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("원본 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(feed).getId();
            FeedUpdateRequest request = new FeedUpdateRequest("수정된 컨텐츠");

            // When
            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedId.toString()))
                .andExpect(jsonPath("$.content").value("수정된 컨텐츠"));

            Feed updated = feedRepository.findById(feedId).orElseThrow();
            assertThat(updated.getContent()).isEqualTo("수정된 컨텐츠");
        }

        @Test
        void 존재하지_않는_피드를_수정하면_404가_반환된다() throws Exception {
            // Given
            UUID missingFeedId = UUID.randomUUID();

            FeedUpdateRequest request = new FeedUpdateRequest("수정");

            // When
            mockMvc.perform(
                    patch("/api/feeds/{feedId}", missingFeedId)
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isNotFound());
        }

        @Test
        void 작성자가_아니면_403이_반환된다() throws Exception {
            // Given
            Feed seed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("원본 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(seed).getId();
            User other = userRepository.save(UserFixture.createUserWithDefault());
            FeedUpdateRequest request = new FeedUpdateRequest("수정 시도");

            // When
            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(other.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isForbidden());
        }

        @Test
        void 내용이_null이거나_blank면_400이_반환된다() throws Exception {
            // Given
            Feed feed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("원본 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(feed).getId();
            FeedUpdateRequest nullRequest = new FeedUpdateRequest(null);
            FeedUpdateRequest blankRequest = new FeedUpdateRequest("   ");

            // When
            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(nullRequest))
                )
                // Then
                .andExpect(status().isBadRequest());

            // When
            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(authorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(blankRequest))
                )
                // Then
                .andExpect(status().isBadRequest());
        }

        @Test
        void 존재하지_않는_작성자가_수정하면_404가_반환된다() throws Exception {
            // Given
            UUID missingAuthorId = UUID.randomUUID();

            Feed feed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("원본 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(feed).getId();
            FeedUpdateRequest request = new FeedUpdateRequest("수정");

            // When
            mockMvc.perform(
                    patch("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(missingAuthorId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // Then
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("피드 삭제 통합 테스트")
    class FeedDeleteIntegrationTests {

        @Test
        void 작성자가_피드를_삭제하면_204와_Soft_Delete가_반영된다() throws Exception {
            // Given
            Feed feed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("삭제 대상 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(feed).getId();

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(authorId)))
                )
                // Then
                .andExpect(status().isNoContent());

            Feed deleted = feedRepository.findById(feedId).orElseThrow();
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        void 존재하지_않는_피드를_삭제하면_404가_반환된다() throws Exception {
            UUID missingFeedId = UUID.randomUUID();

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}", missingFeedId)
                        .with(csrf())
                        .with(user(principal(authorId)))
                )
                // Then
                .andExpect(status().isNotFound());
        }

        @Test
        void 작성자가_아니면_403이_반환된다() throws Exception {
            // Given
            Feed feed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("삭제 대상 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(feed).getId();
            User other = userRepository.save(UserFixture.createUserWithDefault());

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(other.getId())))
                )
                // Then
                .andExpect(status().isForbidden());
        }

        @Test
        void 존재하지_않는_작성자가_삭제하면_404가_반환된다() throws Exception {
            // Given
            UUID missingUserId = UUID.randomUUID();

            Feed feed = Feed.builder()
                .author(author)
                .weather(weather)
                .content("삭제 대상 컨텐츠")
                .createdAt(Instant.now())
                .build();
            UUID feedId = feedRepository.save(feed).getId();

            // When
            mockMvc.perform(
                    delete("/api/feeds/{feedId}", feedId)
                        .with(csrf())
                        .with(user(principal(missingUserId)))
                )
                // Then
                .andExpect(status().isNotFound());
        }
    }
}
