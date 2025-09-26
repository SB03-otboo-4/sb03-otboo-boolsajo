package com.sprint.otboo.feed.event;

import java.util.UUID;

public record FeedCommentedEvent(
    UUID feedAuthorId,
    UUID commentedByUserId,
    UUID commentId
) {

}
