package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import java.util.List;
import java.util.UUID;

public interface FeedRepositoryCustom {
    List<Feed> searchByKeyword(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    );

    long countByFilters(
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    );
}

