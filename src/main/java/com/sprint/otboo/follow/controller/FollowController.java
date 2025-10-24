package com.sprint.otboo.follow.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.request.FollowCreateRequest;
import com.sprint.otboo.follow.dto.response.FollowCreateResponse;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.dto.response.FollowSummaryResponse;
import com.sprint.otboo.follow.service.FollowService;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.service.UserQueryService;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowController implements FollowApi {

    private final FollowService service;
    private final UserQueryService userQueryService;

    public FollowController(FollowService service, UserQueryService userQueryService) {
        this.service = service;
        this.userQueryService = userQueryService;
    }

    @Override
    @PostMapping("")
    public ResponseEntity<FollowCreateResponse> create(@Valid @RequestBody FollowCreateRequest request) {
        UUID followerId = requireUserIdFromSecurityContext();

        // 1) 팔로우 관계 생성 (서비스는 기존대로 FollowDto 반환)
        FollowDto dto = service.create(followerId, request.followeeId());

        // 2) 응답에 들어갈 요약정보 조회
        UserSummaryResponse followerSummary = userQueryService.getSummary(followerId);
        UserSummaryResponse followeeSummary = userQueryService.getSummary(request.followeeId());

        // 3) 프로토타입 스키마로 응답 생성 (200 OK)
        FollowCreateResponse body = new FollowCreateResponse(
            dto.id(),
            followeeSummary,
            followerSummary
        );
        return ResponseEntity.ok(body);
    }

    @Override
    @GetMapping("/summary")
    public ResponseEntity<FollowSummaryResponse> getSummary(
        @RequestParam(value = "userId", required = false) UUID userId
    ) {
        UUID viewerId = requireUserIdFromSecurityContext();
        UUID targetId = (userId != null) ? userId : viewerId;

        FollowSummaryResponse resp = service.getSummary(targetId, viewerId);
        return ResponseEntity.ok(resp);
    }

    // 공통 추출 로직
    private UUID requireUserIdFromSecurityContext() {
        Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new FollowException(
                ErrorCode.UNAUTHORIZED
            );
        }

        Object principal = auth.getPrincipal();

        // 1) CustomUserDetails 타입이면 우선 사용
        try {
            if (principal instanceof CustomUserDetails cud) {
                Object id = cud.getUserId(); // UUID 또는 String 가정
                return (id instanceof UUID)
                    ? (UUID) id
                    : UUID.fromString(String.valueOf(id));
            }
        } catch (Exception ignore) {
            // 다음 단계로 폴백
        }

        // 2) 리플렉션: getId() 메서드가 있으면 사용 (테스트의 PrincipalWithId 커버)
        try {
            Method m = principal.getClass().getMethod("getId");
            Object id = m.invoke(principal);
            return (id instanceof UUID)
                ? (UUID) id
                : UUID.fromString(String.valueOf(id));
        } catch (Exception ignore) {
            // 다음 단계로 폴백
        }

        // 3) OAuth2 JWT 스타일: principal이 Jwt인 경우 userId → sub
        try {
            if (principal instanceof Jwt jwt) {
                String val = jwt.getClaimAsString("userId");
                if (val == null || val.isBlank()) val = jwt.getSubject();
                return UUID.fromString(val);
            }
        } catch (Exception ignore) {
            // 다음 단계로 폴백
        }

        // 4) name()이 UUID 문자열인 경우
        try {
            return UUID.fromString(auth.getName());
        } catch (Exception ignore) {
            // 마지막 폴백 실패 → 401
        }

        throw new FollowException(
            ErrorCode.UNAUTHORIZED
        );
    }

    @Override
    @GetMapping("/followings")
    public ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowings(
        @RequestParam("followerId") UUID followerId,
        @RequestParam(value = "cursor", required = false) String cursor,
        @RequestParam(value = "idAfter", required = false) UUID idAfter,
        @RequestParam(value = "limit", defaultValue = "20") int limit,
        @RequestParam(value = "nameLike", required = false) String nameLike
    ) {
        try {
            validateCursorOrThrow(cursor);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("cursor 파라미터 형식이 올바르지 않습니다.");
        }
        return ResponseEntity.ok(
            service.getFollowings(followerId, cursor, idAfter, boundedLimit(limit), nameLike)
        );
    }

    @GetMapping("/followers")
    public ResponseEntity<CursorPageResponse<FollowListItemResponse>> getFollowers(
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String nameLike
    ) {
        UUID me = requireUserIdFromSecurityContext();
        try {
            validateCursorOrThrow(cursor);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("cursor 파라미터 형식이 올바르지 않습니다.");
        }
        return ResponseEntity.ok(
            service.getFollowers(me, cursor, idAfter, boundedLimit(limit), nameLike)
        );
    }

    @DeleteMapping("/{followId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable UUID followId) {
        UUID me = requireUserIdFromSecurityContext();
        service.unfollowById(me, followId);
    }

    private int boundedLimit(int limit) {
        if (limit < 1) return 1;
        if (limit > 100) return 100;
        return limit;
    }

    private void validateCursorOrThrow(String cursor) {
        if (cursor == null || cursor.isBlank()) return;
        // 형식만 검증 (실제 파싱은 Repository에서 다시 수행)
        Instant.parse(cursor);
    }
}
