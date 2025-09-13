package com.sprint.otboo.feed.dto.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    Author author,
    Weather weather,
    List<OotdItem> ootds,
    String content,
    long likeCount,
    int commentCount,
    boolean likedByMe
) {

    public record Author(
        UUID userId,
        String name,
        String profileImageUrl
    ) {

    }

    public record Weather(
        UUID weatherId,
        String skyStatus,
        Precipitation precipitation,
        Temperature temperature
    ) {

        public record Precipitation(
            String type,
            double amount,
            double probability
        ) {

        }

        public record Temperature(
            double current,
            double comparedToDayBefore,
            double min,
            double max
        ) {

        }
    }

    public record OotdItem(
        UUID clothesId,
        String name
    ) {

    }
}
