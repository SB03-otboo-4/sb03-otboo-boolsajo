package com.sprint.otboo.feed.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.fixture.ClothesFixture;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 테스트")
public class FeedServiceTest {

    @Mock
    FeedRepository feedRepository;
    @Mock
    FeedMapper feedMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    WeatherRepository weatherRepository;
    @Mock
    ClothesRepository clothesRepository;

    @InjectMocks
    FeedServiceImpl feedService;

    @Nested
    @DisplayName("피드 등록 테스트")
    class FeedCreateTests {

        @Test
        void 피드를_등록하면_DTO가_반환된다() {
            // Given
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();
            UUID feedId = UUID.randomUUID();

            User author = UserFixture.create(authorId, "홍길동", "profile.png");
            Weather weather = WeatherFixture.create(weatherId);
            Clothes clothes = ClothesFixture.create(clothesId, author, "셔츠", "image.png",
                ClothesType.TOP);

            FeedCreateRequest request = FeedFixture.createRequest(authorId, weatherId,
                List.of(clothesId), "오늘의 코디");

            Instant now = Instant.now();
            Feed savedFeed = FeedFixture.createEntity(feedId, author, weather, "오늘의 코디", now, now);

            FeedDto expected = FeedFixture.createDto(
                feedId, now, now,
                authorId, "홍길동", "profile.png",
                weatherId, "맑음",
                "비", 0.0, 0.0,
                25.0, -1.0, 20.0, 27.0,
                clothesId, "셔츠", "image.png", ClothesType.TOP,
                "오늘의 코디", 10L, 2, false
            );

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
            given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);
            given(clothesRepository.findAllByIdInAndUser_Id(List.of(clothesId), authorId))
                .willReturn(List.of(clothes));
            given(feedMapper.toDto(savedFeed)).willReturn(expected);

            // When
            FeedDto result = feedService.create(request);

            // Then
            assertThat(result).isSameAs(expected);
            then(userRepository).should().findById(authorId);
            then(weatherRepository).should().findById(weatherId);
            then(feedRepository).should().save(any(Feed.class));
            then(feedMapper).should().toDto(savedFeed);
            then(userRepository).shouldHaveNoMoreInteractions();
            then(weatherRepository).shouldHaveNoMoreInteractions();
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(feedMapper).shouldHaveNoMoreInteractions();
        }

        @Test
        void 작성자가_없으면_피드_등록을_실패한다() {
            // Given
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();

            FeedCreateRequest request = new FeedCreateRequest(
                authorId, weatherId, List.of(clothesId), "오늘의 코디"
            );

            given(userRepository.findById(authorId)).willReturn(java.util.Optional.empty());
            // When / Then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("피드 좋아요 등록 테스트")
    class FeedLikeCreateTests {

        @Test
        void 좋아요를_등록하면_likeCount가_1_증가한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            User liker = User.builder().id(userId).build();
            Feed feed = Feed.builder()
                .id(feedId)
                .author(User.builder().id(UUID.randomUUID()).build())
                .content("hi")
                .likeCount(0L)
                .commentCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(liker));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
            given(feedLikeRepository.existsByFeed_IdAndUser_Id(feedId, userId)).willReturn(false);
            given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));

            // When
            long result = feedService.like(feedId, userId);

            // Then
            assertThat(result).isEqualTo(1L);
            assertThat(feed.getLikeCount()).isEqualTo(1L);
            then(feedRepository).should().save(feed);
            then(feedLikeRepository).shouldHaveNoMoreInteractions();
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(userRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_존재하지_않으면_예외가_발생한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> feedService.like(feedId, userId))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다.");
        }
    }
}
