package com.sprint.otboo.feedsearch.batch;

import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feedsearch.bootstrap.EsIndexBootstrapper;
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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate txTemplate;
    private final EsIndexBootstrapper esIndexBootstrapper;

    @PersistenceContext
    private EntityManager em;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Instant cursorUpdatedAt = Instant.EPOCH;
    private UUID cursorId = new UUID(0, 0);

    @Value("${app.index.run-on-startup:true}")
    private boolean runOnStartup;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void scheduledReindex() {
        runSafely("scheduled", false, true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnceOnStartup() {
        if (!runOnStartup) {
            log.info("[FeedIndexBatchService] 앱 기동 시 색인 비활성화됨 (app.index.run-on-startup=false)");
            return;
        }
        runSafely("startup", false, true);
    }

    /**
     * [FeedIndexBatchService] 안전 실행 래퍼
     * - 동시 실행 방지
     * - 필요 시 커서 초기화(풀 리인덱스)
     * - 필요 시 프리플라이트(인덱스 보장) 수행
     */
    private void runSafely(String reason, boolean fullReindex, boolean preflight) {
        if (!running.compareAndSet(false, true)) {
            log.warn("[FeedIndexBatchService] 색인이 이미 실행 중이어서 건너뜀 (reason={})", reason);
            return;
        }
        try {
            if (preflight) {
                preflightEnsure();
            }
            if (fullReindex) {
                resetCursor();
            }
            reindex();
        } catch (Exception e) {
            log.error("[FeedIndexBatchService] 색인 실패 (reason={})", reason, e);
        } finally {
            running.set(false);
        }
    }

    /**
     * [FeedIndexBatchService] 프리플라이트: 인덱스 보장
     * - 부트스트랩퍼를 통해 인덱스가 없으면 생성한다.
     * - 도커 재기동 등 초기 상태에서도 안전하게 동작.
     */
    private void preflightEnsure() {
        try {
            esIndexBootstrapper.ensure();
            log.debug("[FeedIndexBatchService] 프리플라이트 완료 (인덱스 보장)");
        } catch (Exception e) {
            throw new IllegalStateException("[FeedIndexBatchService] 프리플라이트 실패: 인덱스가 준비되지 않음", e);
        }
    }

    private void resetCursor() {
        this.cursorUpdatedAt = Instant.EPOCH;
        this.cursorId = new UUID(0, 0);
        log.info("[FeedIndexBatchService] 커서를 초기화했습니다. (full reindex)");
    }

    public void reindex() {
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
                    log.info("[FeedIndexBatchService] 피드 색인 진행건수: {}", totalDone);
                }
                feedIndexer.refresh();
                log.info("[FeedIndexBatchService] 피드 색인 완료. total={}", totalDone);
            } catch (Exception e) {
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
