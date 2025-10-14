package com.sprint.otboo.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * 커서 기반 사용자 목록 조회 조건
 * <p> cursor와 idAfter는 함께 넘기면 안 되며, limit은 1~50 범위를 권장 </p>*/
public record UserListQueryParams(
    String cursor,
    String idAfter,
    @Min(1) Integer limit,
    @Pattern(regexp = "email|createdAt") String sortBy,
    @Pattern(regexp = "ASCENDING|DESCENDING") String sortDirection,
    String emailLike,
    String roleEqual,
    Boolean locked
) {
    private static final int DEFAULT_LIMIT = 20;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESCENDING";

    public UserListQueryParams withDefaults() {
        return new UserListQueryParams(
            cursor,
            idAfter,
            limit != null ? limit : DEFAULT_LIMIT,
            StringUtils.hasText(sortBy) ? sortBy : DEFAULT_SORT_BY,
            StringUtils.hasText(sortDirection) ? sortDirection : DEFAULT_SORT_DIRECTION,
            emailLike,
            roleEqual,
            locked
        );
    }

    public UUID parsedIdAfter() {
        return StringUtils.hasText(idAfter) ? UUID.fromString(idAfter) : null;
    }

    public boolean hasRoleEqual() {
        return StringUtils.hasText(roleEqual);
    }
}
