package com.sprint.otboo.feed.mapper;

import com.sprint.otboo.clothing.dto.data.OotdDto;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.user.mapper.AuthorMapper;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import java.time.Instant;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    uses = {AuthorMapper.class, ClothesMapper.class, WeatherMapper.class}
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

    @Mappings({
        @Mapping(target = "author", source = "author"),
        @Mapping(target = "weather", source = "weather"),
        @Mapping(target = "ootds", source = "feedClothes"),
        @Mapping(target = "likedByMe", constant = "false"),
        @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toMillis"),
        @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "toMillis")
    })
    FeedDoc toDoc(Feed feed);

    @Named("toMillis")
    public static Instant toMillis(Instant t) {
        return (t == null) ? null : Instant.ofEpochMilli(t.toEpochMilli());
    }
}
