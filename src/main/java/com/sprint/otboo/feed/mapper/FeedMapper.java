package com.sprint.otboo.feed.mapper;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.user.mapper.UserMapper;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, ClothesMapper.class, WeatherMapper.class}
)
public interface FeedMapper {

    @Mappings({
        @Mapping(target = "author", source = "author"),
        @Mapping(target = "weather", source = "weather"),
        @Mapping(target = "ootds", source = "feedClothes"),
        @Mapping(target = "likeCount", source = "likeCount"),
        @Mapping(target = "commentCount", source = "commentCount"),
        @Mapping(target = "likedByMe", constant = "false")
    })
    FeedDto toDto(Feed feed);

    default OotdDto map(FeedClothes feedClothes, @Context ClothesMapper clothesMapper) {
        if (feedClothes == null || feedClothes.getClothes() == null) {
            return null;
        }
        return clothesMapper.toOotdDto(feedClothes);
    }
}
