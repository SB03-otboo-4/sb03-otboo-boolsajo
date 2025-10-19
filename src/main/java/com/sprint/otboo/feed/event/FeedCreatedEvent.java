package com.sprint.otboo.feed.event;

import java.util.UUID;

public record FeedCreatedEvent(
    UUID feedId,
    UUID authorId
) {

}
