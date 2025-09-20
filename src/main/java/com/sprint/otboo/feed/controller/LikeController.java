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
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{feedId}/like")
    public ResponseEntity<Void> like(
        @PathVariable UUID feedId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID userId = user.getUserId();
        likeService.addLike(feedId, userId);
        return ResponseEntity.noContent().build();
    }
}
