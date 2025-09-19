package com.sprint.otboo.fixture;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.entity.Weather;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class FeedFixture {

    public static FeedCreateRequest createRequest(UUID authorId, UUID weatherId,
        List<UUID> clothesIds, String content) {
        return new FeedCreateRequest(authorId, weatherId, clothesIds, content);
    }

    public static Feed createEntity(UUID id, User author, Weather weather, String content,
        Instant createdAt, Instant updatedAt) {
        return Feed.builder()
            .id(id)
            .author(author)
            .weather(weather)
            .content(content)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public static Feed createEntity(User author, Weather weather, String content,
        Instant createdAt, Instant updatedAt) {
        return Feed.builder()
            .author(author)
            .weather(weather)
            .content(content)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    public static Feed createWithId(UUID id) {
        return Feed.builder()
            .id(id)
            .build();
    }

    public static FeedDto createDto(
        UUID feedId,
        Instant createdAt,
        Instant updatedAt,
        UUID authorId,
        String authorName,
        String profileImageUrl,
        UUID weatherId,
        String weatherSummary,
        String precipType,
        double precipAmount,
        double precipProb,
        double tempCurrent, double tempCompared, double tempMin, double tempMax,
        UUID clothesId, String clothesName, String clothesImageUrl, ClothesType clothesType,
        String content, long likeCount, int commentCount, boolean likedByMe
    ) {
        return new FeedDto(
            feedId,
            createdAt,
            updatedAt,
            new AuthorDto(authorId, authorName, profileImageUrl),
            new WeatherSummaryDto(
                weatherId,
                weatherSummary,
                new PrecipitationDto(precipType, precipAmount, precipProb),
                new TemperatureDto(tempCurrent, tempCompared, tempMin, tempMax)
            ),
            List.of(new OotdDto(clothesId, clothesName, clothesImageUrl, clothesType, List.of())),
            content,
            likeCount,
            commentCount,
            likedByMe
        );
    }

    public static Feed createAt(User author, Weather weather, Instant createdAt) {
        Instant now = Instant.now();
        return Feed.builder()
            .author(author)
            .weather(weather)
            .content("기본 컨텐츠")
            .likeCount(0L)
            .commentCount(0)
            .createdAt(createdAt)
            .updatedAt(now)
            .build();
    }

    public static Feed createWithLikeCount(User author, Weather weather, long likeCount) {
        Instant now = Instant.now();
        return Feed.builder()
            .author(author)
            .weather(weather)
            .content("기본 컨텐츠")
            .likeCount(likeCount)
            .commentCount(0)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public static Feed createWithContent(User author, Weather weather, Instant createdAt,
        String content) {
        return Feed.builder()
            .author(author)
            .weather(weather)
            .content(content)
            .likeCount(0L)
            .commentCount(0)
            .createdAt(createdAt)
            .updatedAt(createdAt)
            .build();
    }
}
