package com.sprint.otboo.follow.controller;

import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.follow.FollowException;
import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.request.FollowCreateRequest;
import com.sprint.otboo.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService service;

    public FollowController(FollowService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowDto create(
        @Valid @RequestBody FollowCreateRequest request,
        @AuthenticationPrincipal(expression = "username") String username
    ) {
        if (username == null) {
            throw new FollowException(ErrorCode.UNAUTHORIZED);
        }
        UUID followerId;
        try {
            followerId = UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            throw new FollowException(ErrorCode.UNAUTHORIZED);
        }
        return service.create(followerId, request.followeeId());
    }
}
