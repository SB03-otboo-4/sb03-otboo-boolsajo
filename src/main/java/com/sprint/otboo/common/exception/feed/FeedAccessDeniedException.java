package com.sprint.otboo.common.exception.feed;

import com.sprint.otboo.common.exception.ErrorCode;
import java.util.UUID;

public class FeedAccessDeniedException extends FeedException {
    public FeedAccessDeniedException() {
        super(ErrorCode.FEED_DENIED);
    }

    public static FeedAccessDeniedException withAuthorIdAndFeedId(UUID authorId, UUID feedId) {
        FeedAccessDeniedException ex = new FeedAccessDeniedException();
        ex.addDetail("authorId", authorId);
        ex.addDetail("feedId", feedId);
        return ex;
    }
}
