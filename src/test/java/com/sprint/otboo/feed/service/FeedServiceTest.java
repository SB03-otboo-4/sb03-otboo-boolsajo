package com.sprint.otboo.feed.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.feed.FeedAccessDeniedException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.fixture.ClothesFixture;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
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
import org.mockito.ArgumentCaptor;
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
            // When & Then
            assertThatThrownBy(() -> feedService.create(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("피드 수정 테스트")
    class FeedUpdateTests {

        @Test
        void 피드를_수정하면_DTO가_반환된다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();

            User author = UserFixture.create(authorId, "홍길동", "profile.png");
            Weather weather = WeatherFixture.create(weatherId);

            Feed existing = FeedFixture.createEntity(
                feedId, author, weather, "오늘의 코디", Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(60)
            );

            String newContent = "오늘의 코디(수정)";
            FeedUpdateRequest request = new FeedUpdateRequest(newContent);

            FeedDto expected = FeedFixture.createDto(
                feedId, Instant.now(), Instant.now(),
                authorId, "홍길동", "profile.png",
                weatherId, "맑음",
                "비", 0.0, 0.0,
                25.0, -1.0, 20.0, 27.0,
                UUID.randomUUID(), "셔츠", "image.png", ClothesType.TOP,
                newContent, 10L, 2, false
            );

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(existing));
            given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));
            given(feedMapper.toDto(any(Feed.class))).willReturn(expected);

            // When
            FeedDto result = feedService.update(authorId, feedId, request);

            // Then
            assertThat(result).isSameAs(expected);
            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);
            then(feedRepository).should().save(any(Feed.class));
            then(feedMapper).should().toDto(any(Feed.class));
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(feedMapper).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_없으면_예외를_반환한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            FeedUpdateRequest request = new FeedUpdateRequest("수정 내용");

            User author = UserFixture.create(authorId, "홍길동", "profile.png");

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> feedService.update(authorId, feedId, request))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다");

            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);
            then(feedRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 작성자가_아니면_수정에_실패한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            User otherAuthor = UserFixture.create(otherUserId, "임꺽정", "p2.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Feed existing = FeedFixture.createEntity(
                feedId, otherAuthor, weather, "원본", Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(60)
            );

            FeedUpdateRequest request = new FeedUpdateRequest("수정 내용");

            given(userRepository.findById(authorId)).willReturn(Optional.of(otherAuthor));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(existing));

            // When & Then
            assertThatThrownBy(() -> feedService.update(authorId, feedId, request))
                .isInstanceOf(FeedAccessDeniedException.class)
                .hasMessageContaining("피드에 대한 권한이 없습니다");

            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);
            then(feedRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("피드 삭제 테스트 (Soft Delete)")
    class FeedDeleteTests {

        @Test
        void 작성자가_본인_피드를_삭제하면_성공한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();

            User author = UserFixture.create(authorId, "홍길동", "profile.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Feed existing = FeedFixture.createEntity(
                feedId, author, weather, "삭제 대상",
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
            );

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(existing));
            given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));

            // When
            feedService.delete(authorId, feedId);

            // Then
            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);

            ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
            then(feedRepository).should().save(captor.capture());
            Feed saved = captor.getValue();

            assertThat(saved.isDeleted()).isTrue();

            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);
            then(feedRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_없으면_예외를_반환한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            User author = UserFixture.create(authorId, "홍길동", "profile.png");

            given(userRepository.findById(authorId)).willReturn(Optional.of(author));
            given(feedRepository.findById(feedId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> feedService.delete(authorId, feedId))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다");

            then(userRepository).should().findById(authorId);
            then(feedRepository).should().findById(feedId);
            then(feedRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 해당_피드의_작성자가_아니면_예외를_반환한다() {
            // Given
            UUID feedId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            User requester = UserFixture.create(requesterId, "임꺽정", "p2.png");
            User owner = UserFixture.create(ownerId, "홍길동", "p1.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Feed existing = FeedFixture.createEntity(
                feedId, owner, weather, "원본",
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
            );

            given(userRepository.findById(requesterId)).willReturn(Optional.of(requester));
            given(feedRepository.findById(feedId)).willReturn(Optional.of(existing));

            // When & Then
            assertThatThrownBy(() -> feedService.delete(requesterId, feedId))
                .isInstanceOf(FeedAccessDeniedException.class)
                .hasMessageContaining("피드에 대한 권한이 없습니다");

            then(userRepository).should().findById(requesterId);
            then(feedRepository).should().findById(feedId);
            then(feedRepository).shouldHaveNoMoreInteractions();
        }
    }
}
