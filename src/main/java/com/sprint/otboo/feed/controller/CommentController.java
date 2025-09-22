package com.sprint.otboo.feed.controller;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import com.sprint.otboo.feed.service.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class CommentController implements CommentApi {

    private final CommentService commentService;

    @Override
    @PostMapping("/{feedId}/comments")
    public ResponseEntity<CommentDto> create(@PathVariable UUID feedId,
        @Valid @RequestBody CommentCreateRequest request) {
        log.info("[CommentController] 댓글 생성 요청: feedId={}, authorId={}, content={}",
            feedId, request.authorId(), request.content());
        CommentDto dto = commentService.create(request.authorId(), feedId, request.content());

        log.info("[CommentController] 댓글 생성 완료: feedId={}, commentId={}", feedId, dto.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    @GetMapping("/{feedId}/comments")
    public ResponseEntity<CursorPageResponse<CommentDto>> getComments(
        @PathVariable UUID feedId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit
    ) {
        log.info("[CommentController] 댓글 조회 요청: feedId={}, cursor={}, idAfter={}, limit={}",
            feedId, cursor, idAfter, limit);
        CursorPageResponse<CommentDto> body = commentService.getComments(feedId, cursor, idAfter,
            limit);

        log.info("[CommentController] 댓글 조회 완료: feedId={}, count={}, hasNext={}",
            feedId, body.data().size(), body.hasNext()
        );

        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}
