package com.sprint.otboo.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 알림 커서 페이지네이션 응답 DTO
 * 프론트엔드가 다음 페이지 요청 시 cursor와 idAfter를 그대로 사용할 수 있도록 설계
 */
public record NotificationCursorResponse(
    List<NotificationDto> data,
    @JsonProperty("nextCursor")
    String cursor,
    @JsonProperty("nextIdAfter")
    String idAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

    /**
     * CursorPageResponse로부터 NotificationCursorResponse 생성
     */
    public static NotificationCursorResponse from(
        List<NotificationDto> data,
        String nextCursor,
        String nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
    ) {
        return new NotificationCursorResponse(
            data,
            nextCursor,
            nextIdAfter,
            hasNext,
            totalCount,
            sortBy,
            sortDirection
        );
    }
}