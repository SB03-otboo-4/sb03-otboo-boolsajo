package com.sprint.otboo.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.user.dto.data.UserDto;
import java.util.List;

public record UserDtoCursorResponse(
    @JsonProperty("data")
    List<UserDto> data,
    String nextCursor,
    String nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {
    public static UserDtoCursorResponse from(CursorPageResponse<UserDto> src) {
        return new UserDtoCursorResponse(
            src.data(),
            src.nextCursor(),
            src.nextIdAfter(),
            src.hasNext(),
            src.totalCount(),
            src.sortBy(),
            src.sortDirection()
        );
    }
}
