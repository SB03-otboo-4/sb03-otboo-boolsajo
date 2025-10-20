package com.sprint.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

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
import com.sprint.otboo.feedsearch.event.FeedChangedEvent;
import com.sprint.otboo.feedsearch.event.FeedDeletedEvent;
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
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock
    private ApplicationEventPublisher publisher;
    @InjectMocks
    FeedServiceImpl feedService;

    @Nested
    @DisplayName("피드 등록 테스트")
    class FeedCreateTests {

        @Test
        void 피드를_등록하면_DTO가_반환된다() {
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "profile.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Clothes clothes = ClothesFixture.create(UUID.randomUUID(), author, "셔츠", "image.png",
                ClothesType.TOP);

            FeedCreateRequest request = FeedFixture.createRequest(
                author.getId(), weather.getId(), List.of(clothes.getId()), "오늘의 코디"
            );

            Feed savedFeed = FeedFixture.createEntity(null, author, weather, "오늘의 코디",
                Instant.now(), Instant.now());

            FeedDto expected = FeedFixture.createDto(
                savedFeed.getId(), Instant.now(), Instant.now(),
                author.getId(), "홍길동", "profile.png",
                weather.getId(), "맑음",
                "비", 0.0, 0.0,
                25.0, -1.0, 20.0, 27.0,
                clothes.getId(), "셔츠", "image.png", ClothesType.TOP,
                "오늘의 코디", 10L, 2, false
            );

            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(weatherRepository.findById(weather.getId())).willReturn(Optional.of(weather));
            given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);
            given(
                clothesRepository.findAllByIdInAndUser_Id(List.of(clothes.getId()), author.getId()))
                .willReturn(List.of(clothes));
            given(feedMapper.toDto(savedFeed)).willReturn(expected);

            // when
            FeedDto result = feedService.create(request);

            // then
            assertThat(result).isSameAs(expected);
            verify(publisher).publishEvent(any(FeedChangedEvent.class));
            then(userRepository).should().findById(author.getId());
            then(weatherRepository).should().findById(weather.getId());
            then(feedRepository).should().save(any(Feed.class));
            then(feedMapper).should().toDto(savedFeed);
            then(userRepository).shouldHaveNoMoreInteractions();
            then(weatherRepository).shouldHaveNoMoreInteractions();
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(feedMapper).shouldHaveNoMoreInteractions();
        }

        @Test
        void 작성자가_없으면_피드_등록을_실패한다() {
            // given
            UUID authorId = UUID.randomUUID();
            UUID weatherId = UUID.randomUUID();
            UUID clothesId = UUID.randomUUID();

            FeedCreateRequest request = new FeedCreateRequest(
                authorId, weatherId, List.of(clothesId), "오늘의 코디"
            );
            given(userRepository.findById(authorId)).willReturn(Optional.empty());

            // when & then
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
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "profile.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Feed feed = FeedFixture.createEntity(
                author, weather, "오늘의 코디", Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(60)
            );
            String newContent = "오늘의 코디(수정)";
            FeedUpdateRequest request = new FeedUpdateRequest(newContent);

            FeedDto expected = FeedFixture.createDto(
                feed.getId(), Instant.now(), Instant.now(),
                author.getId(), "홍길동", "profile.png",
                weather.getId(), "맑음",
                "비", 0.0, 0.0,
                25.0, -1.0, 20.0, 27.0,
                UUID.randomUUID(), "셔츠", "image.png", ClothesType.TOP,
                newContent, 10L, 2, false
            );

            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(feedRepository.findById(feed.getId())).willReturn(Optional.of(feed));
            given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));
            given(feedMapper.toDto(any(Feed.class))).willReturn(expected);

            // when
            FeedDto result = feedService.update(author.getId(), feed.getId(), request);

            // then
            assertThat(result).isSameAs(expected);
            verify(publisher).publishEvent(any(FeedChangedEvent.class));
            then(userRepository).should().findById(author.getId());
            then(feedRepository).should().findById(feed.getId());
            then(feedRepository).should().save(any(Feed.class));
            then(feedMapper).should().toDto(any(Feed.class));
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(feedMapper).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_없으면_예외를_반환한다() {
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "profile.png");
            FeedUpdateRequest request = new FeedUpdateRequest("수정 내용");

            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.update(author.getId(), UUID.randomUUID(), request))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다");

            then(userRepository).should().findById(author.getId());
            then(feedRepository).should().findById(any(UUID.class));
            then(feedRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 작성자가_아니면_수정에_실패한다() {
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "p1.png");
            User otherAuthor = UserFixture.create(UUID.randomUUID(), "임꺽정", "p2.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());

            Feed feed = FeedFixture.createEntity(
                author, weather, "원본", Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(60)
            );

            FeedUpdateRequest request = new FeedUpdateRequest("수정 내용");

            given(userRepository.findById(otherAuthor.getId())).willReturn(
                Optional.of(otherAuthor));
            given(feedRepository.findById(feed.getId())).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.update(otherAuthor.getId(), feed.getId(), request))
                .isInstanceOf(FeedAccessDeniedException.class)
                .hasMessageContaining("피드에 대한 권한이 없습니다");

            then(userRepository).should().findById(otherAuthor.getId());
            then(feedRepository).should().findById(feed.getId());
            assertThat(feed.getContent()).isEqualTo("원본");
        }
    }

    @Nested
    @DisplayName("피드 삭제 테스트 (Soft Delete)")
    class FeedDeleteTests {

        @Test
        void 작성자가_본인_피드를_삭제하면_성공한다() {
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "profile.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Feed feed = FeedFixture.createEntity(
                author, weather, "삭제 대상",
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(60)
            );

            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(feedRepository.findById(feed.getId())).willReturn(Optional.of(feed));
            given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            feedService.delete(author.getId(), feed.getId());

            // then
            then(userRepository).should().findById(author.getId());
            then(feedRepository).should().findById(feed.getId());

            ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
            then(feedRepository).should().save(captor.capture());
            Feed saved = captor.getValue();
            assertThat(saved.isDeleted()).isTrue();

            verify(publisher).publishEvent(any(FeedDeletedEvent.class));
            then(userRepository).should().findById(author.getId());
            then(feedRepository).should().findById(feed.getId());
            then(feedRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 피드가_없으면_예외를_반환한다() {
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "profile.png");
            given(userRepository.findById(author.getId())).willReturn(Optional.of(author));
            given(feedRepository.findById(any(UUID.class))).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedService.delete(author.getId(), UUID.randomUUID()))
                .isInstanceOf(FeedNotFoundException.class)
                .hasMessageContaining("피드를 찾을 수 없습니다");

            then(userRepository).should().findById(author.getId());
            then(feedRepository).should().findById(any(UUID.class));
            then(feedRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        void 해당_피드의_작성자가_아니면_예외를_반환한다() {
            // given
            User author = UserFixture.create(UUID.randomUUID(), "홍길동", "p1.png");
            User otherAuthor = UserFixture.create(UUID.randomUUID(), "임꺽정", "p2.png");
            Weather weather = WeatherFixture.create(UUID.randomUUID());
            Feed feed = FeedFixture.createEntity(
                author, weather, "원본",
                Instant.now().minusSeconds(120), Instant.now().minusSeconds(60)
            );

            given(userRepository.findById(otherAuthor.getId())).willReturn(
                Optional.of(otherAuthor));
            given(feedRepository.findById(feed.getId())).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.delete(otherAuthor.getId(), feed.getId()))
                .isInstanceOf(FeedAccessDeniedException.class)
                .hasMessageContaining("피드에 대한 권한이 없습니다");

            then(userRepository).should().findById(otherAuthor.getId());
            then(feedRepository).should().findById(feed.getId());
            then(feedRepository).shouldHaveNoMoreInteractions();
        }
    }
}
