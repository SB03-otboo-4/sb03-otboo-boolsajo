package com.sprint.otboo.feed.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.mapper.ClothesMapperImpl;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.fixture.WeatherLocationFixture;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.mapper.AuthorMapperImpl;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.mapper.WeatherMapperImpl;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    FeedMapperImpl.class,
    AuthorMapperImpl.class,
    ClothesMapperImpl.class,
    WeatherMapperImpl.class
})
class FeedMapperTest {

    @Autowired
    FeedMapper feedMapper;

    private User user;
    private WeatherLocation location;
    private Weather weather;
    private List<FeedClothes> feedClothes;
    private Feed feed;

    @BeforeEach
    void setUp() {
        // Given
        user = UserFixture.createUserWithDefault();
        location = WeatherLocationFixture.createLocationWithDefault();
        weather = WeatherFixture.createWeatherWithDefault(location);

        Clothes clothes1 = Clothes.builder()
            .id(UUID.randomUUID())
            .name("Navy Jacket")
            .type(ClothesType.OUTER)
            .build();

        Clothes clothes2 = Clothes.builder()
            .id(UUID.randomUUID())
            .name("White Sneakers")
            .type(ClothesType.SHOES)
            .build();

        FeedClothes feedClothes1 = FeedClothes.builder()
            .id(UUID.randomUUID())
            .clothes(clothes1)
            .build();

        FeedClothes feedClothes2 = FeedClothes.builder()
            .id(UUID.randomUUID())
            .clothes(clothes2)
            .build();

        feedClothes = List.of(feedClothes1, feedClothes2);

        feed = Feed.builder()
            .id(UUID.randomUUID())
            .author(user)
            .weather(weather)
            .feedClothes(feedClothes)
            .likeCount(7L)
            .commentCount(3L)
            .createdAt(Instant.parse("2025-03-02T12:34:56Z"))
            .updatedAt(Instant.parse("2025-03-03T01:02:03Z"))
            .build();
    }

    @Test
    void Feed을_FeedDto로_정확히_매핑한다() {
        // Given
        Feed source = feed;

        // When & Then
        FeedDto feedDto = feedMapper.toDto(source);

        AuthorDto authorDto = feedDto.author();
        assertThat(authorDto).isNotNull();
        assertThat(authorDto.name()).isEqualTo("홍길동");

        WeatherSummaryDto weatherDto = feedDto.weather();
        assertThat(weatherDto).isNotNull();
        assertThat(weatherDto.skyStatus()).isEqualTo("CLEAR");

        List<OotdDto> ootdDtos = feedDto.ootds();
        assertThat(ootdDtos).isNotNull().hasSize(2);
        assertThat(ootdDtos).extracting(OotdDto::name)
            .containsExactlyInAnyOrder("Navy Jacket", "White Sneakers");

        assertThat(feedDto.likeCount()).isEqualTo(7L);
        assertThat(feedDto.commentCount()).isEqualTo(3L);
        assertThat(feedDto.likedByMe()).isFalse();
    }

    @Test
    void Feed을_FeedDoc으로_매핑하고_epochMillis_보존과_likedByMe_false를_검증한다() {
        // Given
        Feed source = feed;

        // When
        FeedDoc feedDoc = feedMapper.toDoc(source);

        // Then
        assertThat(feedDoc).isNotNull();
        assertThat(feedDoc.author()).isNotNull();
        assertThat(feedDoc.weather()).isNotNull();
        assertThat(feedDoc.ootds()).isNotNull().hasSize(2);
        assertThat(feedDoc.likedByMe()).isFalse();

        assertThat(feedDoc.createdAt()).isNotNull();
        assertThat(feedDoc.updatedAt()).isNotNull();
        assertThat(feedDoc.createdAt().toEpochMilli())
            .isEqualTo(feed.getCreatedAt().toEpochMilli());
        assertThat(feedDoc.updatedAt().toEpochMilli())
            .isEqualTo(feed.getUpdatedAt().toEpochMilli());
    }
}
