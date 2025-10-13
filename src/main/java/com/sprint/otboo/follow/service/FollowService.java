package com.sprint.otboo.follow.service;

import com.sprint.otboo.follow.dto.data.FollowDto;
import java.util.UUID;

public interface FollowService {

    FollowDto create(UUID followerId, UUID followeeId);
}
