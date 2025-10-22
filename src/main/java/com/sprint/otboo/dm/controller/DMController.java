package com.sprint.otboo.dm.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.dto.response.DirectMessageProtoResponse;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.service.UserQueryService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/direct-messages")
public class DMController implements DMApi {

    private final DMService service;
    private final UserQueryService userQueryService;

    @Override
    public ResponseEntity<CursorPageResponse<DirectMessageProtoResponse>> getList(
        UUID otherUserId,
        String cursor,
        UUID idAfter,
        Integer limit
    ) {
        UUID me = requireUserIdFromSecurityContext();

        if (me.equals(otherUserId)) {
            throw new DMException(ErrorCode.SELF_DM_NOT_ALLOWED);
        }
        if (org.springframework.util.StringUtils.hasText(cursor)) {
            try { Instant.parse(cursor); }
            catch (Exception e) { throw new DMException(ErrorCode.INVALID_CURSOR_FORMAT); }
        }
        if (idAfter != null && !org.springframework.util.StringUtils.hasText(cursor)) {
            throw new DMException(ErrorCode.INVALID_CURSOR_PAIR);
        }
        if (limit != null && (limit < 1 || limit > 100)) {
            throw new DMException(ErrorCode.INVALID_PAGING_LIMIT);
        }

        CursorPageResponse<DirectMessageDto> page =
            service.getDms(me, otherUserId, cursor, idAfter, limit);

        java.util.List<DirectMessageProtoResponse> mapped = page.data().stream().map(d -> {
            UserSummaryResponse s = userQueryService.getSummary(d.senderId());
            UserSummaryResponse r = userQueryService.getSummary(d.receiverId());
            return DirectMessageProtoResponse.from(d, s, r);
        }).toList();

        CursorPageResponse<DirectMessageProtoResponse> body =
            new CursorPageResponse<>(
                mapped,
                page.nextCursor(),
                page.nextIdAfter(),
                page.hasNext(),
                page.totalCount(),
                page.sortBy(),
                page.sortDirection()
            );

        return ResponseEntity.ok(body);
    }

    private UUID requireUserIdFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new DMException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = auth.getPrincipal();
        try {
            if (principal instanceof CustomUserDetails cud) {
                Object id = cud.getUserId();
                return (id instanceof UUID) ? (UUID) id : UUID.fromString(String.valueOf(id));
            }
            Method getId = principal.getClass().getMethod("getId");
            Object id = getId.invoke(principal);
            return (id instanceof UUID) ? (UUID) id : UUID.fromString(String.valueOf(id));
        } catch (Exception e) {
            throw new DMException(ErrorCode.UNAUTHORIZED);
        }
    }
}
