package com.sprint.otboo.feedsearch.batch;

import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedIndexBatchService {

    private static final int PAGE_SIZE = 1000;

    private final FeedMapper feedMapper;
    private final FeedIndexer feedIndexer;
    private final TransactionTemplate txTemplate;

    @PersistenceContext
    private EntityManager em;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Instant cursorUpdatedAt = Instant.EPOCH;
    private UUID cursorId = new UUID(0, 0);

    @Value("${app.index.run-on-startup:true}")
    private boolean runOnStartup;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void scheduledReindex() {
        runSafely("scheduled", false);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnceOnStartup() {
        if (!runOnStartup) {
            log.info("앱 기동 시 색인 비활성화되어 있습니다. (app.index.run-on-startup=false)");
            return;
        }
        runSafely("startup", false);
    }

    private void runSafely(String reason, boolean fullReindex) {
        if (!running.compareAndSet(false, true)) {
            log.warn("피드 색인이 이미 실행 중입니다. 건너뜁니다. (reason={})", reason);
            return;
        }
        try {
            if (fullReindex) {
                resetCursor();
            }
            reindex();
        } catch (Exception e) {
            log.error("색인 실패 (reason={})", reason, e);
        } finally {
            running.set(false);
        }
    }

    private void resetCursor() {
        this.cursorUpdatedAt = Instant.EPOCH;
        this.cursorId = new UUID(0, 0);
        log.info("커서를 초기화했습니다. (full reindex)");
    }

    public void reindex() throws IOException {
        txTemplate.executeWithoutResult(status -> {
            try {
                long totalDone = 0L;
                while (true) {
                    List<Feed> chunk = fetchNextChunk(PAGE_SIZE);
                    if (chunk.isEmpty()) break;

                    List<FeedDoc> docs = chunk.stream()
                        .map(feedMapper::toDoc)
                        .toList();

                    feedIndexer.bulkUpsert(docs);
                    advanceCursor(chunk);
                    em.clear();

                    totalDone += docs.size();
                    log.info("피드 색인 진행건수: {}", totalDone);
                }
                feedIndexer.refresh();
                log.info("피드 색인 완료. total={}", totalDone);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<Feed> fetchNextChunk(int size) {
        return em.createQuery("""
                    select f from Feed f
                    left join fetch f.author
                    left join fetch f.weather
                    where f.deleted = false
                      and (
                            f.updatedAt > :u
                         or (f.updatedAt = :u and f.id > :id)
                      )
                    order by f.updatedAt asc, f.id asc
                """, Feed.class)
            .setParameter("u", cursorUpdatedAt)
            .setParameter("id", cursorId)
            .setMaxResults(size)
            .getResultList();
    }

    private void advanceCursor(List<Feed> chunk) {
        Feed tail = chunk.get(chunk.size() - 1);
        cursorUpdatedAt = tail.getUpdatedAt();
        cursorId = tail.getId();
    }
}
