package com.sprint.otboo.feed.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.CommentDto;
import java.util.UUID;

public interface CommentService {

    CommentDto create(UUID authorId, UUID feedId, String content);

    CursorPageResponse<CommentDto> getComments(UUID feedId, String cursor, UUID idAfter, int limit);
}
