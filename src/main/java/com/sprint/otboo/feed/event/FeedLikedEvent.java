package com.sprint.otboo.feed.event;

import java.util.UUID;

public record FeedLikedEvent(
    UUID feedAuthorId,
    UUID likedByUserId
) {

}
