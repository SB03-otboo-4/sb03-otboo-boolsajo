package com.sprint.otboo.feed.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import java.util.UUID;

public interface FeedService {

    FeedDto create(FeedCreateRequest request);

    void addLike(UUID feedId, UUID userId);

    CursorPageResponse<FeedDto> getFeeds(String cursor,
        int limit,
        String sortBy,
        String sortDirection,
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType);
}
