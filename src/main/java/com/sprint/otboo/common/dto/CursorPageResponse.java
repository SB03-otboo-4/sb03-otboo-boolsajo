package com.sprint.otboo.common.dto;

import java.util.List;

public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    String nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

}
