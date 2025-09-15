package com.sprint.otboo.feed.controller;

import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.service.FeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class FeedController implements FeedApi {

    private final FeedService feedService;

    @PostMapping
    public ResponseEntity<FeedDto> create(@Valid @RequestBody FeedCreateRequest request) {
        log.info("[FeedController] 피드 생성 요청: authorId={}, weatherId={}",
            request.authorId(), request.weatherId());

        FeedDto dto = feedService.create(request);
        log.info("[FeedController] 피드 생성 완료: feedId={}", dto.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
