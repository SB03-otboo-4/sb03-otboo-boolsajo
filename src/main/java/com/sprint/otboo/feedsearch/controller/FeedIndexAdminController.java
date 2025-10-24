package com.sprint.otboo.feedsearch.controller;

import com.sprint.otboo.feedsearch.batch.FeedIndexBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class FeedIndexAdminController {

    private final FeedIndexBatchService batch;

    /** 전체 재색인 트리거 */
    @PostMapping("/full-reindex")
    public void fullReindex() {
        batch.resetCursorForReindex();
    }
}