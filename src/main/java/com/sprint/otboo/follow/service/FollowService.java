package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import java.util.UUID;

public interface FollowService {

    FollowDto create(UUID followerId, UUID followeeId);

    FollowSummaryDto getMySummary(UUID userId);

    CursorPageResponse<FollowListItemResponse> getFollowings(
        UUID followerId, String cursor, UUID idAfter, int limit, String nameLike
    );

    CursorPageResponse<FollowListItemResponse> getFollowers(
        UUID userId, String cursor, UUID idAfter, int limit, String nameLike
    );
}
