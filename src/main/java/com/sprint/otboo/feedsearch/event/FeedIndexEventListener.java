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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedIndexEventListener {

    private final ElasticsearchClient es;
    private final FeedRepository feedRepository;
    private final FeedMapper feedMapper;

    private static final String INDEX_ALIAS = "feed-write";

    @Value("${search.index.refresh-policy:WAIT_FOR}")
    private String refreshPolicy;

    private Refresh refresh() {
        return switch (refreshPolicy) {
            case "TRUE"  -> Refresh.True;
            case "FALSE" -> Refresh.False;
            default      -> Refresh.WaitFor;
        };
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(FeedChangedEvent e) {
        try {
            Feed f = feedRepository.findById(e.feedId()).orElse(null);
            if (f == null) {
                onDeleted(new FeedDeletedEvent(e.feedId()));
                return;
            }

            FeedDoc doc = feedMapper.toDoc(f);

            es.index(i -> i.index(INDEX_ALIAS)
                .id(doc.id().toString())
                .document(doc)
                .refresh(refresh())
                .timeout(t -> t.time("2s")));

            log.debug("[FeedIndexEventListener] upsert 성공: feedId={}, index={}", e.feedId(), INDEX_ALIAS);
        } catch (Exception ex) {
            log.error("[FeedIndexEventListener] upsert 실패: feedId={}", e.feedId(), ex);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(FeedDeletedEvent e) {
        try {
            es.delete(d -> d.index(INDEX_ALIAS)
                .id(e.feedId().toString())
                .refresh(refresh()));
            log.debug("[FeedIndexEventListener] delete 성공: feedId={}, index={}", e.feedId(), INDEX_ALIAS);
        } catch (Exception ex) {
            log.error("[FeedIndexEventListener] delete 실패: feedId={}", e.feedId(), ex);
        }
    }
}
