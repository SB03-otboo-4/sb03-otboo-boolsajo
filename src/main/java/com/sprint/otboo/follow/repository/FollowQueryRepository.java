package com.sprint.otboo.follow.repository;

import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import java.util.List;
import java.util.UUID;

public interface FollowQueryRepository {

    List<FollowListItemResponse> findFollowingPage(
        UUID followerId,
        String cursorCreatedAtIso,
        UUID idAfter,
        int limitPlusOne,
        String nameLike
    );

    long countFollowing(UUID followerId, String nameLike);
}
