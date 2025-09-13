package com.sprint.otboo.feed;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 테스트")
public class FeedServiceTest {

    @Test
    @DisplayName("피드를_등록하면_DTO가_반환된다")
    void 피드를_등록하면_DTO가_반환된다() {
        // Given
        UUID authorId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();

        User authorRef = User.builder().id(authorId).build();
        Weather weatherRef = Weather.builder().id(weatherId).build();
        FeedCreateRequest request =
            new FeedCreateRequest(authorId, weatherId, List.of(clothesId), "오늘의 코디");

        Feed feed = Feed.builder()
            .id(UUID.randomUUID())
            .author(authorRef)
            .weather(weatherRef)
            .content("오늘의 코디")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        FeedDto expected = new FeedDto(
            UUID.randomUUID(),
            Instant.now(),
            Instant.now(),
            new FeedDto.Author(authorId, "홍길동", "profile.png"),
            new FeedDto.Weather(
                weatherId,
                "맑음",
                new FeedDto.Weather.Precipitation("비", 0.0, 0.0),
                new FeedDto.Weather.Temperature(25.0, -1.0, 20.0, 27.0)
            ),
            List.of(new FeedDto.OotdItem(clothesId, "셔츠")),
            "오늘의 코디",
            10L,
            2,
            false
        );


        given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);
        given(feedMapper.toDto(savedFeed)).willReturn(expected);

        // When
        FeedDto result = feedService.create(request);

        // Then
        assertThat(result).isSameAs(expected);
        then(feedRepository).should().save(any(Feed.class));
        then(feedMapper).should().toDto(savedFeed);
        then(feedRepository).shouldHaveNoMoreInteractions();
        then(feedMapper).shouldHaveNoMoreInteractions();
    }
}
