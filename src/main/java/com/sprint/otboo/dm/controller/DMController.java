package com.sprint.otboo.dm.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.service.DMService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/direct-messages")
public class DMController {

    private final DMService service;

    @GetMapping("")
    public ResponseEntity<CursorPageResponse<DirectMessageDto>> getList(
        @RequestParam("userId") UUID otherUserId,
        @RequestParam(value = "cursor", required = false) String cursor,
        @RequestParam(value = "idAfter", required = false) UUID idAfter,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        UUID me = requireUserIdFromSecurityContext();

        // (1) 자기 자신과의 대화 금지
        if (me.equals(otherUserId)) {
            throw new DMException(ErrorCode.SELF_DM_NOT_ALLOWED);
        }

        // (2) cursor 형식(ISO-8601) 검증
        if (StringUtils.hasText(cursor)) {
            try {
                Instant.parse(cursor);
            } catch (Exception e) {
                throw new DMException(ErrorCode.INVALID_CURSOR_FORMAT);
            }
        }

        // (3) idAfter가 있으면 cursor도 필수
        if (idAfter != null && !StringUtils.hasText(cursor)) {
            throw new DMException(ErrorCode.INVALID_CURSOR_PAIR);
        }

        // (4) limit 범위 검증 (1..100)
        if (limit != null && (limit < 1 || limit > 100)) {
            throw new DMException(ErrorCode.INVALID_PAGING_LIMIT);
        }

        CursorPageResponse<DirectMessageDto> resp =
            service.getDms(me, otherUserId, cursor, idAfter, limit);

        return ResponseEntity.ok(resp);
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
