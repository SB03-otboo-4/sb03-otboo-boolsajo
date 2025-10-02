package com.sprint.otboo.follow.controller;

import com.sprint.otboo.follow.dto.data.FollowDto;
import com.sprint.otboo.follow.dto.reqeust.FollowCreateRequest;
import com.sprint.otboo.follow.service.FollowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService service;

    public FollowController(FollowService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowDto create(@Valid @RequestBody FollowCreateRequest request) {
        return service.create(request.followerId(), request.followeeId());
    }
}
