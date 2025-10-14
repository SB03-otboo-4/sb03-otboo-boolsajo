package com.sprint.otboo.feedsearch.event;

import java.util.UUID;

public record FeedChangedEvent(
    UUID feedId
) {

}
