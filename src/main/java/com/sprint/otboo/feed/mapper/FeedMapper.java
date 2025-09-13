package com.sprint.otboo.feed.mapper;

import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.entity.Feed;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedMapper {

    @Mapping(target = "author.userId", source = "author.id")
    @Mapping(target = "weather.weatherId", source = "weather.id")
    FeedDto toDto(Feed feed);
}
