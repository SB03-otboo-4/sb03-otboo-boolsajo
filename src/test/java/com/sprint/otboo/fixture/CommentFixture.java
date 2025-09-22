package com.sprint.otboo.fixture;

import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.user.entity.User;
import java.time.Instant;
import java.util.UUID;

public class CommentFixture {

    public static Comment create(UUID id, User author, Feed feed, String content,
        Instant createdAt) {
        return Comment.builder()
            .id(id)
            .author(author)
            .feed(feed)
            .content(content)
            .createdAt(createdAt)
            .build();
    }
}
