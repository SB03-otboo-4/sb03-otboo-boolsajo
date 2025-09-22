package com.sprint.otboo.feed.service;

import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import java.util.UUID;

public interface FeedService {

    FeedDto create(FeedCreateRequest request);

    FeedDto update(UUID authorId, UUID feedId, FeedUpdateRequest request);
}
