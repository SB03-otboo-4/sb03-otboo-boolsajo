package com.sprint.otboo.feed.service;

import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import java.util.UUID;

public interface FeedService {

    FeedDto create(FeedCreateRequest request);

    void addLike(UUID feedId, UUID userId);
}
