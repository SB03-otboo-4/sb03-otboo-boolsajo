package com.sprint.otboo.follow.event;

import java.util.UUID;

public record FollowCreatedEvent(
    UUID followerId,
    UUID followeeId
) {

}
