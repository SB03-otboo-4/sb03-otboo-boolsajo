package com.sprint.otboo.feedsearch.batch;

import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feedsearch.bootstrap.EsIndexBootstrapper;
import com.sprint.otboo.feedsearch.dto.CursorDto;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.feedsearch.redis.RedisCursorHelper;
import com.sprint.otboo.feedsearch.redis.RedisLockHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 배치 기반 피드 색인 서비스.
 *
 * <p>특징:
 * <ul>
 *   <li>분산 락(Redis)으로 단일 실행 보장</li>
 *   <li>키셋 페이지네이션(updatedAt, id)으로 재시작 가능(resumable) 처리</li>
 *   <li>삭제 동기화(soft delete 반영), Bulk Upsert, 주기적 Refresh</li>
 *   <li>커서를 Redis에 영속화하여 재기동/다중 인스턴스 환경에서 이어서 처리</li>
 * </ul>
 *
 * <p>주의:
 * <ul>
 *   <li>커서는 <b>성공</b> 시에만 전진/저장한다.</li>
 *   <li>IOException 발생 시 예외를 전파하여 배치를 중단한다(다음 스케줄에서 재시도).</li>
 * </ul>
 *
 * @author …
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeedIndexBatchService {

    /**
     * 한 번에 처리할 페이지 크기(문서 수).
     */
    private static final int PAGE_SIZE = 500;

    private final FeedMapper feedMapper;
    private final FeedIndexer feedIndexer;
    private final EsIndexBootstrapper esIndexBootstrapper;
    private final RedisCursorHelper redis;
    private final FeedRepository feedRepository;
    private final TransactionTemplate txTemplate;
    private final RedisLockHelper redisLockHelper;

    @PersistenceContext
    private EntityManager em;

    /**
     * 내부 처리용 청크 컨테이너.
     *
     * @param docs          업서트 대상 문서
     * @param lastUpdatedAt 커서 전진 기준이 되는 마지막 updatedAt
     * @param lastId        커서 전진 기준이 되는 마지막 id
     */
    private record Chunk(List<FeedDoc> docs, Instant lastUpdatedAt, UUID lastId) {

    }

    /**
     * 진행 커서 (영속: Redis).
     * <p>초기값은 epoch(1970-01-01T00:00:00Z, UUID 000…)로 시작한다.</p>
     */
    private CursorDto cursor = CursorDto.epoch();

    @Value("${app.index.run-on-startup:false}")
    private boolean runOnStartup;
    @Value("${app.index.write-alias:feed-write}")
    private String writeAlias;
    @Value("${app.index.lock.ttl-seconds:60}")
    private long lockTtlSeconds;

    /**
     * 분산 락 키를 반환한다.
     *
     * @return redis lock key (e.g., {@code locks:feed:index:feed-write})
     */
    private String lockKey() {
        return "locks:feed:index:" + writeAlias;
    }

    /**
     * 애플리케이션 기동 시 색인을 실행한다.
     * <p>{@code app.index.run-on-startup=false} 이면 아무 작업도 하지 않는다.</p>
     */
    public void runStartup() {
        if (!runOnStartup) {
            log.info("[FeedIndexBatchService] 앱 기동 시 색인 비활성화됨");
            return;
        }
        loadCursor();
        run("startup");
    }

    /**
     * 커서를 초기화하고 전체 재색인을 수행한다.
     */
    public void resetCursorForReindex() {
        ensureIndexReady();

        boolean started = redisLockHelper.runWithLock(
            lockKey(),
            Duration.ofSeconds(lockTtlSeconds),
            () -> {
                log.info("[FeedIndexBatchService] Full Reindex 시작: 커서 리셋");
                resetCursor();
                reindex();
            }
        );

        if (!started) {
            log.warn("[FeedIndexBatchService] Full Reindex 스킵: 다른 인스턴스가 수행 중");
        }
    }

    /**
     * 스케줄러에 의해 주기적으로 호출되는 진입점.
     * <p>실행 전 커서를 로드하고 분산 락을 획득한 인스턴스에서만 실제 색인을 수행한다.</p>
     */
    public void runScheduled() {
        loadCursor();
        run("scheduled");
    }

    /**
     * 분산 락 하에 프리플라이트 검사 후 색인을 수행한다.
     *
     * @param reason 호출 사유 (로그용)
     * @throws IllegalStateException 인덱스가 준비되지 않은 경우
     */
    private void run(String reason) {
        ensureIndexReady();
        boolean started = redisLockHelper.runWithLock(
            lockKey(),
            Duration.ofSeconds(lockTtlSeconds),
            () -> {
                try {
                    // 인덱스가 비어 있으면 커서를 초기화하여 전체 재색인
                    if (feedIndexer.count() == 0L) {
                        log.info("[FeedIndexBatchService] 인덱스 비어있음 → 커서 리셋");
                        resetCursor();
                    }
                } catch (IOException e) {
                    log.warn("[FeedIndexBatchService] 인덱스 카운트 실패: {}", e.getMessage());
                }
                reindex();
            }
        );
        if (!started) {
            log.warn("[FeedIndexBatchService] 다른 인스턴스 수행 중 (reason={})", reason);
        }
    }

    /**
     * 메인 루프를 돌며 삭제 동기화 → 청크 조회 → Bulk Upsert → 커서 전진을 반복한다.
     *
     * <ol>
     *   <li>삭제 동기화: 소프트 삭제된 엔티티를 색인에서 제거</li>
     *   <li>키셋 페이지네이션으로 다음 청크 조회</li>
     *   <li>ES Bulk Upsert</li>
     *   <li>커서 전진 & Redis 저장</li>
     *   <li>EM clear로 1차 캐시 정리</li>
     * </ol>
     *
     * @implNote IOException 발생 시 예외를 전파하여 배치를 중단한다. (커서는 성공 시에만 전진하므로, 다음 실행에서 동일 지점부터 재개)
     */
    public void reindex() {
        long totalUpserts = 0L;
        long totalDeletes = 0L;

        while (true) {
            // 1) 삭제 동기화
            List<UUID> deletedIds = txTemplate.execute(status ->
                feedRepository.findDeletedIdsSince(
                    cursor.updatedAt(),
                    cursor.id(),
                    PageRequest.of(0, PAGE_SIZE)
                )
            );
            if (deletedIds != null && !deletedIds.isEmpty()) {
                try {
                    feedIndexer.bulkDelete(deletedIds);
                    totalDeletes += deletedIds.size();
                } catch (IOException e) {
                    // 일시 오류 포함 모든 IO 실패는 경고만 로깅하고 루프 진행(업서트 단계에서 실패 시 중단됨)
                    log.warn("[FeedIndexBatchService] bulkDelete 실패: {}", e.getMessage(), e);
                }
            }

            // 2) 다음 청크 조회 (키셋 페이지네이션)
            Chunk chunk = txTemplate.execute(status -> {
                List<Feed> feeds = fetchNextChunk(PAGE_SIZE);
                if (feeds.isEmpty()) {
                    return new Chunk(List.of(), cursor.updatedAt(), cursor.id());
                }

                List<FeedDoc> docs = feeds.stream().map(feedMapper::toDoc).toList();
                Feed tail = feeds.get(feeds.size() - 1);

                // 5) 메모리 정리
                em.clear();

                return new Chunk(docs, tail.getUpdatedAt(), tail.getId());
            });

            if (chunk.docs().isEmpty()) {
                break;
            }

            // 3) 업서트
            try {
                feedIndexer.bulkUpsert(chunk.docs());
            } catch (IOException e) {
                // 업서트 실패는 배치를 중단(커서 전진 전이라 안전)
                throw new UncheckedIOException(e);
            }

            // 4) 커서 전진/저장
            this.cursor = new CursorDto(chunk.lastUpdatedAt(), chunk.lastId());
            redis.saveCursor(writeAlias, cursor.updatedAt(), cursor.id());

            totalUpserts += chunk.docs().size();
            log.info(
                "[FeedIndexBatchService] 진행: upsert(acc)={}, delete(acc)={}, lastCursor=({}, {})",
                totalUpserts, totalDeletes, cursor.updatedAt(), cursor.id()
            );
        }

        try {
            feedIndexer.refresh();
        } catch (IOException e) {
            log.warn("[FeedIndexBatchService] refresh 실패(색인 완료): {}", e.getMessage(), e);
        }
        log.info("[FeedIndexBatchService] 완료. upserts={}, deletes={}", totalUpserts, totalDeletes);
    }

    /**
     * 키셋 페이지네이션 조건으로 다음 배치 청크를 조회한다.
     *
     * @param size 최대 조회 건수
     * @return 정렬된 다음 페이지의 엔티티 목록
     */
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
            .setParameter("u", cursor.updatedAt())
            .setParameter("id", cursor.id())
            .setMaxResults(size)
            .getResultList();
    }

    private void ensureIndexReady() {
        try {
            esIndexBootstrapper.ensure();
            log.debug("[FeedIndexBatchService] 프리플라이트 완료: 인덱스 보장");
        } catch (Exception e) {
            throw new IllegalStateException("[FeedIndexBatchService] 프리플라이트 실패: 인덱스가 준비되지 않음", e);
        }
    }

    private void resetCursor() {
        cursor = CursorDto.epoch();
        redis.saveCursor(writeAlias, cursor.updatedAt(), cursor.id());
        log.info("[FeedIndexBatchService] 커서 초기화 (Redis 반영)");
    }

    private void loadCursor() {
        cursor = redis.loadCursor(writeAlias).orElse(CursorDto.epoch());
        log.info("[FeedIndexBatchService] 커서 로드: updatedAt={}, id={}", cursor.updatedAt(),
            cursor.id());
    }
}
