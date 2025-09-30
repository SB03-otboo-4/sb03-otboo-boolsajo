package com.sprint.otboo.feedsearch.batch;

import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedIndexBatchService {

    private static final int PAGE_SIZE = 1000;

    private final FeedMapper feedMapper;
    private final FeedIndexer feedIndexer;

    @PersistenceContext
    private EntityManager em;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Instant cursorUpdatedAt = Instant.EPOCH;
    private UUID cursorId = new UUID(0, 0);

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional(readOnly = true)
    public void scheduledReindex() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Feed reindex already running. skip.");
            return;
        }
        try {
            reindex();
        } catch (Exception e) {
            log.error("Scheduled reindex failed", e);
        } finally {
            running.set(false);
        }
    }

    @Transactional(readOnly = true)
    public void reindex() throws IOException {
        long totalDone = 0L;

        while (true) {
            List<Feed> chunk = fetchNextChunk(PAGE_SIZE);
            if (chunk.isEmpty()) {
                break;
            }

            List<FeedDoc> docs = chunk.stream()
                .map(feedMapper::toDoc)
                .toList();

            feedIndexer.bulkUpsert(docs);
            advanceCursor(chunk);
            em.clear();

            totalDone += docs.size();
            log.info("Feed reindex progress: {}", Long.valueOf(totalDone));
        }

        feedIndexer.refresh();
        log.info("Feed reindex finished. total={}", Long.valueOf(totalDone));
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
