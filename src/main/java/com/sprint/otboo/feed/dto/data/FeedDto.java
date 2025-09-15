package com.sprint.otboo.feed.dto.data;

import com.sprint.otboo.clothing.dto.data.OotdItemDto;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.weather.dto.data.FeedWeatherDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    AuthorDto author,
    FeedWeatherDto weather,
    List<OotdItemDto> ootds,
    String content,
    long likeCount,
    int commentCount,
    boolean likedByMe
) {

}
