package com.sprint.otboo.feed.controller;

import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import com.sprint.otboo.feed.service.CommentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{feedId}/comments")
    public ResponseEntity<CommentDto> create(@PathVariable UUID feedId,
        @Valid @RequestBody CommentCreateRequest request) {
        CommentDto dto = commentService.create(request.authorId(), feedId, request.content());

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
