package com.sprint.otboo.fixture;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FeedDocFixture {

    private FeedDocFixture() {
    }

    public static FeedDoc createWithDefault(UUID id) {
        Instant now = Instant.parse("2025-09-01T00:00:00Z");

        AuthorDto author = new AuthorDto(
            UUID.randomUUID(),
            "홍길동",
            "https://example.com/profile.png"
        );

        WeatherSummaryDto weather = new WeatherSummaryDto(
            UUID.randomUUID(),
            "CLEAR",
            new PrecipitationDto("NONE", 0.0, 0.0),
            new TemperatureDto(20.0, 0.0, 18.0, 25.0)
        );

        OotdDto ootd = new OotdDto(
            UUID.randomUUID(),
            "기본 상의",
            "https://example.com/clothes.png",
            ClothesType.TOP,
            List.of()
        );

        return new FeedDoc(
            id,
            now,
            now,
            author,
            weather,
            List.of(ootd),
            "기본 컨텐츠",
            0L,
            0,
            false
        );
    }
}
