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

    private co.elastic.clients.elasticsearch._types.Refresh refresh() {
        return switch (refreshPolicy) {
            case "TRUE"  -> Refresh.True;
            case "FALSE" -> Refresh.False;
            default      -> Refresh.WaitFor;
        };
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(FeedChangedEvent e) {
        try {
            Feed f = feedRepository.findById(e.feedId()).orElse(null);
            if (f == null ) { onDeleted(new FeedDeletedEvent(e.feedId())); return; }
            FeedDoc doc = feedMapper.toDoc(f);
            es.index(i -> i.index(writeAlias)
                .id(doc.id().toString())
                .document(doc)
                .refresh(refresh())
                .timeout(t -> t.time("2s")));
        } catch (Exception ex) {
            log.error("Index failed for feed {}", e.feedId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(FeedDeletedEvent e) {
        try {
            es.delete(d -> d.index(writeAlias)
                .id(e.feedId().toString())
                .refresh(refresh()));
        } catch (Exception ex) {
            log.error("Delete index failed for feed {}", e.feedId(), ex);
        }
    }
}
