package com.sprint.otboo.feed.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.feed.service.LikeService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class LikeController implements LikeApi {

    private final LikeService likeService;

    @Override
    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> like(
        @PathVariable UUID feedId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.info("[LikeController] 좋아요 등록 요청: feedId={}, userId={}",
            feedId, user.getUserId());
        UUID userId = user.getUserId();
        likeService.addLike(feedId, userId);
        log.info("[FeedController] 좋아요 등록 완료: feedId={}, userId={}",
            feedId, user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
