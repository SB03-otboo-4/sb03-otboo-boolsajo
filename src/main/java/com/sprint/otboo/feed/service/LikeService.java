package com.sprint.otboo.feed.service;

import java.util.UUID;

public interface LikeService {
    void addLike(UUID feedId, UUID userId);
}
