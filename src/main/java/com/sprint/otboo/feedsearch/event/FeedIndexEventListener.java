package com.sprint.otboo.feedsearch.event;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 피드 엔티티 변경 이벤트를 수신하여 Elasticsearch 색인을 유지하는 리스너.
 *
 * <p>트랜잭션 커밋 이후( {@link TransactionPhase#AFTER_COMMIT} )에 작동하며,
 * 생성/수정된 피드는 upsert, 삭제된 피드는 delete로 ES에 반영한다.</p>
 *
 * <h2>동작 개요</h2>
 * <ul>
 *   <li>피드 변경 이벤트 수신 → DB에서 최신 엔티티 재조회 → {@link FeedDoc} 매핑 → ES 인덱스에 upsert</li>
 *   <li>피드 삭제 이벤트 수신 → ES 인덱스에서 문서 삭제</li>
 *   <li>{@link Async} 적용으로 호출 스레드와 분리되어 비동기로 처리</li>
 * </ul>
 *
 * <h2>설정(프로퍼티)</h2>
 * <ul>
 *   <li><code>search.index.write-alias</code> : 쓰기용 인덱스(또는 알리아스) 이름. 기본값 <code>feeds</code></li>
 *   <li><code>search.index.refresh-policy</code> : ES refresh 정책 (TRUE/FALSE/WAIT_FOR). 기본값 WAIT_FOR</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeedIndexEventListener {

    private final ElasticsearchClient es;
    private final FeedRepository feedRepository;
    private final FeedMapper feedMapper;

    @Value("${search.index.write-alias:feeds}")
    private String writeAlias;

    @Value("${search.index.refresh-policy:WAIT_FOR}")
    private String refreshPolicy;

    /**
     * 설정값(<code>search.index.refresh-policy</code>)에 따라 ES {@link Refresh} 값을 반환한다.
     *
     * @return TRUE, FALSE, WAIT_FOR 중 하나
     */
    private Refresh refresh() {
        return switch (refreshPolicy) {
            case "TRUE"  -> Refresh.True;
            case "FALSE" -> Refresh.False;
            default      -> Refresh.WaitFor;
        };
    }

    /**
     * 피드가 생성/수정되었을 때 호출되는 이벤트 핸들러.
     *
     * <p>안전성 확보를 위해 트랜잭션 커밋 이후에 동작하며, 이벤트의 id로 DB에서 최신
     * 엔티티를 재조회한 뒤 ES에 upsert한다. 만약 DB에서 해당 피드를 찾을 수 없으면
     * 삭제 이벤트로 간주하여 ES에서도 삭제를 시도한다.</p>
     *
     * @param e 변경된 피드의 식별자를 담은 이벤트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(FeedChangedEvent e) {
        try {
            Feed f = feedRepository.findById(e.feedId()).orElse(null);
            if (f == null) {
                onDeleted(new FeedDeletedEvent(e.feedId()));
                return;
            }

            FeedDoc doc = feedMapper.toDoc(f);

            es.index(i -> i.index(writeAlias)
                .id(doc.id().toString())
                .document(doc)
                .refresh(refresh())
                .timeout(t -> t.time("2s")));

            log.debug("[FeedIndexEventListener] upsert 성공: feedId={}, index={}", e.feedId(), writeAlias);
        } catch (Exception ex) {
            log.error("[FeedIndexEventListener] upsert 실패: feedId={}", e.feedId(), ex);
        }
    }

    /**
     * 피드가 삭제되었을 때 호출되는 이벤트 핸들러.
     *
     * <p>트랜잭션 커밋 이후에 ES 인덱스에서 해당 문서를 제거한다.</p>
     *
     * @param e 삭제된 피드의 식별자를 담은 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(FeedDeletedEvent e) {
        try {
            es.delete(d -> d.index(writeAlias)
                .id(e.feedId().toString())
                .refresh(refresh()));

            log.debug("[FeedIndexEventListener] delete 성공: feedId={}, index={}", e.feedId(), writeAlias);
        } catch (Exception ex) {
            log.error("[FeedIndexEventListener] delete 실패: feedId={}", e.feedId(), ex);
        }
    }
}
