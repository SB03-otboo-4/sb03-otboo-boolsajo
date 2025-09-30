package com.sprint.otboo.feedsearch.repository;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import java.util.UUID;

public interface FeedSearchRepositoryCustom {

    CursorPageResponse<UUID> searchIds(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType,
        UUID authorId
    );

    long countByFilters(
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType,
        UUID authorId
    );
}
