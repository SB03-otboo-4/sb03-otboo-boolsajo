package com.sprint.otboo.feed.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import java.util.UUID;

public interface FeedService {

    FeedDto create(FeedCreateRequest request);

    CursorPageResponse<FeedDto> getFeeds(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType,
        UUID authorId);

    FeedDto update(UUID authorId, UUID feedId, FeedUpdateRequest request);
}
