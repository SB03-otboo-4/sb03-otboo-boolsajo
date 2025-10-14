package com.sprint.otboo.feedsearch.event;

import java.util.UUID;

public record FeedDeletedEvent(
    UUID feedId
) {

}
