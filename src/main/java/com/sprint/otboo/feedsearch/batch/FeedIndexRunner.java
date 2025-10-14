package com.sprint.otboo.feedsearch.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class FeedIndexRunner {

    private final FeedIndexBatchService service;

    @EventListener(ApplicationReadyEvent.class)
    public void runOnceOnStart() {
        log.info("[FeedIndexRunner] 앱 기동 인덱싱");
        service.runStartup();
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void scheduledReindex() {
        log.info("[FeedIndexRunner] 스케줄 인덱싱");
        service.runScheduled();
    }
}
