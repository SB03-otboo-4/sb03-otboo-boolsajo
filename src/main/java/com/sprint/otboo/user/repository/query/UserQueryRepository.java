package com.sprint.otboo.user.repository.query;

import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.service.support.UserListEnums;
import java.util.UUID;

public interface UserQueryRepository {

    UserSlice findSlice(
        String cursor,
        UUID idAfter,
        int limit,
        UserListEnums.SortBy sortBy,
        UserListEnums.SortDirection sortDirection,
        String emailLike,
        Role roleEqual,
        Boolean locked
    );

    long countAll(
        String emailLike,
        Role roleEqual,
        Boolean locked
    );
}