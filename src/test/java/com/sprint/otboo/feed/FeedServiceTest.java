package com.sprint.otboo.feed;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.data.FeedDto.Author;
import com.sprint.otboo.feed.dto.data.FeedDto.OotdItem;
import com.sprint.otboo.feed.dto.data.FeedDto.Weather.Precipitation;
import com.sprint.otboo.feed.dto.data.FeedDto.Weather.Temperature;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedClothesRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feed.service.FeedServiceImpl;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
    FeedClothesRepository feedClothesRepository;
    @Mock
    ClothesRepository clothesRepository;

    @InjectMocks
    FeedServiceImpl feedService;

    @Test
    @DisplayName("피드를_등록하면_DTO가_반환된다")
    void 피드를_등록하면_DTO가_반환된다() {
        // Given
        UUID authorId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();


        User author = User.builder()
            .id(authorId)
            .username("홍길동")
            .profileImageUrl("profile.png")
            .build();

        Weather weather = Weather.builder()
            .id(weatherId)
            .build();

        Clothes clothes = Clothes.builder()
            .id(clothesId)
            .user(author)
            .name("셔츠")
            .imageUrl("image.png")
            .type(ClothesType.TOP)
            .build();

        FeedCreateRequest request =
            new FeedCreateRequest(authorId, weatherId, List.of(clothesId), "오늘의 코디");

        Feed savedFeed = Feed.builder()
            .id(UUID.randomUUID())
            .author(author)
            .weather(weather)
            .content("오늘의 코디")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        FeedDto expected = new FeedDto(
            UUID.randomUUID(),
            Instant.now(),
            Instant.now(),
            new Author(authorId, "홍길동", "profile.png"),
            new FeedDto.Weather(
                weatherId,
                "맑음",
                new Precipitation("비", 0.0, 0.0),
                new Temperature(25.0, -1.0, 20.0, 27.0)
            ),
            List.of(new OotdItem(clothesId, "셔츠")),
            "오늘의 코디",
            10L,
            2,
            false
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

        FeedCreateRequest req = new FeedCreateRequest(
            authorId, weatherId, List.of(clothesId), "오늘의 코디"
        );

        given(userRepository.getReferenceById(authorId))
            .willThrow(new EntityNotFoundException("User not found: " + authorId));

        // When / Then
        assertThatThrownBy(() -> feedService.create(req))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("User not found");
    }
}
