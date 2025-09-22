package com.sprint.otboo.feed.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.service.FeedService;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class FeedController implements FeedApi {

    private final FeedService feedService;

    @Override
    @PostMapping
    public ResponseEntity<FeedDto> create(@Valid @RequestBody FeedCreateRequest request) {
        log.info("[FeedController] 피드 생성 요청: authorId={}, weatherId={}",
            request.authorId(), request.weatherId());

        FeedDto dto = feedService.create(request);
        log.info("[FeedController] 피드 생성 완료: feedId={}", dto.id());

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<FeedDto>> getFeeds(
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam String sortBy,
        @RequestParam String sortDirection,
        @RequestParam(required = false) String keywordLike,
        @RequestParam(name = "skyStatusEqual", required = false) SkyStatus skyStatusEqual,
        @RequestParam(name = "precipitationTypeEqual", required = false) PrecipitationType precipitationTypeEqual,
        @RequestParam(name = "authorIdEqual", required = false) UUID authorIdEqual
    ) {
        log.info(
            "[FeedController] 피드 목록 조회 요청: cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}, keywordLike={}, skyStatusEqual={}, precipitationTypeEqual={}, authorIdEqual={}",
            cursor, idAfter, limit, sortBy, sortDirection, keywordLike, skyStatusEqual,
            precipitationTypeEqual, authorIdEqual
        );
        CursorPageResponse<FeedDto> body = feedService.getFeeds(
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            keywordLike,
            skyStatusEqual,
            precipitationTypeEqual,
            authorIdEqual
        );

        log.info("[FeedController] 피드 목록 조회 완료: count={}, hasNext={}",
            body.data().size(), body.hasNext());

        return ResponseEntity.ok(body);
    }
}
