package com.sprint.otboo.follow.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.dto.response.FollowSummaryResponse;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.follow.event.FollowCreatedEvent;
import com.sprint.otboo.follow.repository.FollowQueryRepository;
import com.sprint.otboo.follow.repository.FollowRepository;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FollowQueryRepository followQueryRepository;

    public FollowServiceImpl(FollowRepository followRepository, UserRepository userRepository, FollowQueryRepository followQueryRepository, ApplicationEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
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
        eventPublisher.publishEvent(new FollowCreatedEvent(saved.getFollowerId(), saved.getFolloweeId()));
        return new FollowDto(saved.getId(), saved.getFollowerId(), saved.getFolloweeId());
    }

    @Override
    @Transactional(readOnly = true)
    public FollowSummaryResponse getSummary(UUID targetUserId, UUID viewerUserId) {
        long followerCount  = followRepository.countByFolloweeId(targetUserId); // target의 팔로워 수
        long followingCount = followRepository.countByFollowerId(targetUserId); // target의 팔로잉 수

        // viewer → target (내가 대상을 팔로우?)
        Optional<Follow> rel = Optional.empty();
        if (!targetUserId.equals(viewerUserId)) {
            rel = followRepository.findByFollowerIdAndFolloweeId(viewerUserId, targetUserId);
        }
        boolean followedByMe = rel.isPresent();
        UUID followedByMeId = rel.map(Follow::getId).orElse(null);

        // target → viewer (대상이 나를 팔로우?)
        boolean followingMe = false;
        if (!targetUserId.equals(viewerUserId)) {
            followingMe = followRepository.existsByFollowerIdAndFolloweeId(targetUserId, viewerUserId);
        }

        return new FollowSummaryResponse(
            targetUserId,
            followerCount,
            followingCount,
            followedByMe,
            followedByMeId,
            followingMe
        );
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

    @Override
    @Transactional
    public void unfollowById(UUID followerId, UUID followId) {
        Follow follow = followRepository.findById(followId)
            .orElseThrow(() -> new FollowException(ErrorCode.FOLLOW_NOT_FOUND));

        // 내가 만든 팔로우만 지울 수 있게 보호
        if (!follow.getFollowerId().equals(followerId)) {
            throw new FollowException(ErrorCode.FOLLOW_NOT_FOUND);
        }

        followRepository.delete(follow);
    }
}
