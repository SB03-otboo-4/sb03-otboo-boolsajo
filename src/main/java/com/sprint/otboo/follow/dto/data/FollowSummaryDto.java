package com.sprint.otboo.follow.dto.data;

public record FollowSummaryDto(
    long followerCount,   // 나를 팔로우하는 수
    long followingCount   // 내가 팔로우하는 수
) {}
