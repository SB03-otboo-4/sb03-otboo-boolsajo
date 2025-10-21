package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.dto.response.FollowSummaryResponse;
import java.util.UUID;

public interface FollowService {

    FollowDto create(UUID followerId, UUID followeeId);

    FollowSummaryResponse getSummary(UUID targetUserId, UUID viewerUserId);

    CursorPageResponse<FollowListItemResponse> getFollowings(
        UUID followerId, String cursor, UUID idAfter, int limit, String nameLike
    );

    CursorPageResponse<FollowListItemResponse> getFollowers(
        UUID userId, String cursor, UUID idAfter, int limit, String nameLike
    );

    void unfollowById(UUID followerId, UUID followId);
}
