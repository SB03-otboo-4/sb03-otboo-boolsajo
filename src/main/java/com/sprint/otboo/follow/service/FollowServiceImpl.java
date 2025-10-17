package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.repository.FollowQueryRepository;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowQueryRepository followQueryRepository;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository, FollowQueryRepository followQueryRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.followQueryRepository = followQueryRepository;
    }

    @Override
    @Transactional
    public FollowDto create(UUID followerId, UUID followeeId) {

        // 자기 자신 팔로우 금지
        if (followerId.equals(followeeId)) {
            throw new FollowException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        // 사용자 존재 검증
        if (!userRepository.existsById(followerId) || !userRepository.existsById(followeeId)) {
            throw new FollowException(ErrorCode.USER_NOT_FOUND);
        }

        // 중복 팔로우 금지
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new FollowException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }

        Follow saved = followRepository.save(Follow.of(followerId, followeeId));
        return new FollowDto(saved.getId(), saved.getFollowerId(), saved.getFolloweeId());
    }

    @Override
    public FollowSummaryDto getMySummary(UUID userId) {

        long following = followRepository.countByFollowerId(userId);
        long follower  = followRepository.countByFolloweeId(userId);
        return new FollowSummaryDto(follower, following);
    }

    @Override
    public CursorPageResponse<FollowListItemResponse> getFollowings(
        UUID followerId,
        String cursor,
        UUID idAfter,
        int limit,
        String nameLike
    ) {
        int pageSize = (limit <= 0 || limit > 100) ? 20 : limit;

        List<FollowListItemResponse> rows = followQueryRepository.findFollowingPage(
            followerId, cursor, idAfter, pageSize + 1, nameLike
        );

        boolean hasNext = rows.size() > pageSize;
        List<FollowListItemResponse> pageRows = hasNext ? rows.subList(0, pageSize) : rows;

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !pageRows.isEmpty()) {
            FollowListItemResponse last = pageRows.get(pageRows.size() - 1);
            nextCursor = last.createdAt() != null ? last.createdAt().toString() : null;
            nextIdAfter = last.id() != null ? last.id().toString() : null;
        }

        long total = followQueryRepository.countFollowing(followerId, nameLike);

        return new CursorPageResponse<FollowListItemResponse>(
            pageRows,
            nextCursor,
            nextIdAfter,
            hasNext,
            total,
            "createdAt",
            "DESCENDING"
        );
    }

    @Override
    public CursorPageResponse<FollowListItemResponse> getFollowers(
        UUID me, String cursor, UUID idAfter, int limit, String nameLike
    ) {
        // limit 보정: [1, 100]
        int pageSize = limit;
        if (pageSize < 1) pageSize = 1;
        else if (pageSize > 100) pageSize = 100;

        // nameLike 정규화: blank → null
        String normalizedNameLike =
            (nameLike != null && !nameLike.isBlank()) ? nameLike : null;

        // Repository 호출 시 limitPlusOne = pageSize + 1
        List<FollowListItemResponse> rows = followQueryRepository.findFollowersPage(
            me, cursor, idAfter, pageSize + 1, normalizedNameLike
        );

        boolean hasNext = rows.size() > pageSize;
        List<FollowListItemResponse> pageRows = hasNext ? rows.subList(0, pageSize) : rows;

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext) {
            FollowListItemResponse last = pageRows.get(pageRows.size() - 1);
            nextCursor = last.createdAt().toString();
            nextIdAfter = last.id().toString();
        }

        long total = followQueryRepository.countFollowers(me, normalizedNameLike);

        return new CursorPageResponse<>(
            pageRows,
            nextCursor,
            nextIdAfter,
            hasNext,
            total,
            "createdAt",
            "DESCENDING"
        );
    }
}
