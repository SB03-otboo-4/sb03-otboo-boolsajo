package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentRepositoryCustom {

    List<Comment> findByFeedId(
        UUID feedId,
        String cursor,
        UUID idAfter,
        int limit
    );

    long countByFeedId(UUID feedId);
}
