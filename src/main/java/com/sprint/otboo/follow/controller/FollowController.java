package com.sprint.otboo.follow.controller;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.dto.request.FollowCreateRequest;
import com.sprint.otboo.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowController implements FollowApi {

    private final FollowService service;

    public FollowController(FollowService service) {
        this.service = service;
    }

    @Override
    @PostMapping("")
    public ResponseEntity<FollowDto> create(@Valid @RequestBody FollowCreateRequest request) {
        UUID followerId = requireUserIdFromSecurityContext();
        FollowDto dto = service.create(followerId, request.followeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryDto> getSummary() {
        UUID userId = requireUserIdFromSecurityContext();
        return ResponseEntity.ok(service.getMySummary(userId));
    }

    // 공통 추출 로직
    private UUID requireUserIdFromSecurityContext() {
        var ctx = SecurityContextHolder.getContext();
        var auth = (ctx != null) ? ctx.getAuthentication() : null;

        if (auth == null || !auth.isAuthenticated()) {
            throw new FollowException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = auth.getPrincipal();
        try {
            if (principal instanceof com.sprint.otboo.auth.jwt.CustomUserDetails cud) {
                Object id = cud.getUserId();
                return (id instanceof UUID) ? (UUID) id : UUID.fromString(String.valueOf(id));
            }
        } catch (Exception ignored) {
            // 다음 단계로 폴백
        }

        try {
            if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                String val = jwt.getClaimAsString("userId");
                if (val == null || val.isBlank()) val = jwt.getSubject(); // sub
                return UUID.fromString(val);
            }
        } catch (Exception ignored) {}

        try {
            return UUID.fromString(auth.getName());
        } catch (Exception ignored) {}

        throw new FollowException(ErrorCode.UNAUTHORIZED);
    }
}
