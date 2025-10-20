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

    private static final String DEF_AUTHOR_NAME = "홍길동";
    private static final String DEF_PROFILE_URL = "https://example.com/profile.png";
    private static final String DEF_SKY = "CLEAR";
    private static final String DEF_CONTENT = "기본 컨텐츠";
    private static final String DEF_CLOTHES_NAME = "기본 상의";
    private static final String DEF_CLOTHES_IMG = "https://example.com/clothes.png";
    private static final ClothesType DEF_CLOTHES_TYPE = ClothesType.TOP;

    public static FeedDto createDtoWithDefault() {
        UUID feedId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID authorId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();
        return createDto(
            feedId,
            now, now,
            authorId, DEF_AUTHOR_NAME, DEF_PROFILE_URL,
            weatherId, DEF_SKY,
            "NONE", 0.0, 0.0,
            20.0, 0.0, 18.0, 25.0,
            clothesId, DEF_CLOTHES_NAME, DEF_CLOTHES_IMG, DEF_CLOTHES_TYPE,
            DEF_CONTENT, 0L, 0, false
        );
    }

    public static FeedDto createDtoWithCreatedAt(Instant createdAt) {
        UUID feedId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID authorId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();
        return createDto(
            feedId,
            createdAt, now,
            authorId, DEF_AUTHOR_NAME, DEF_PROFILE_URL,
            weatherId, DEF_SKY,
            "NONE", 0.0, 0.0,
            20.0, 0.0, 18.0, 25.0,
            clothesId, DEF_CLOTHES_NAME, DEF_CLOTHES_IMG, DEF_CLOTHES_TYPE,
            DEF_CONTENT, 0L, 0, false
        );
    }

    public static FeedDto createDtoWithLikeCount(Long likeCount) {
        UUID feedId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID authorId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();
        return createDto(
            feedId,
            now, now,
            authorId, DEF_AUTHOR_NAME, DEF_PROFILE_URL,
            weatherId, DEF_SKY,
            "NONE", 0.0, 0.0,
            20.0, 0.0, 18.0, 25.0,
            clothesId, DEF_CLOTHES_NAME, DEF_CLOTHES_IMG, DEF_CLOTHES_TYPE,
            DEF_CONTENT, likeCount, 0, false
        );
    }

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

    public static Feed createEntity(User author, Weather weather) {
        return Feed.builder()
            .author(author)
            .weather(weather)
            .content("컨텐츠")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
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
