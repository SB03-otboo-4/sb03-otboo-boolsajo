package com.sprint.otboo.user.repository.query;

import com.sprint.otboo.user.entity.User;
import java.util.List;
import java.util.UUID;

public record UserSlice(
    List<User> rows,
    boolean hasNext,
    String nextCursor,
    UUID nextIdAfter
) {

}
