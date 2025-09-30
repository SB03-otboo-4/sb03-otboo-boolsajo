package com.sprint.otboo.feedsearch.dto;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public record FeedDoc(
    UUID id,
    @Field(type = FieldType.Date, format = { DateFormat.date_time, DateFormat.epoch_millis })
    Instant createdAt,
    @Field(type = FieldType.Date, format = { DateFormat.date_time, DateFormat.epoch_millis })
    Instant updatedAt,
    AuthorDto author,
    WeatherSummaryDto weather,
    List<OotdDto> ootds,
    String content,
    long likeCount,
    int commentCount,
    boolean likedByMe
) {

}
