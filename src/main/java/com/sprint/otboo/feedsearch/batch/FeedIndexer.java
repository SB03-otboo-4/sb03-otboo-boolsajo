package com.sprint.otboo.feedsearch.batch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedIndexer {

    @Value("${app.index.write-alias:feed-write}")
    private String indexAlias;

    private final ElasticsearchClient es;

    public void bulkUpsert(List<FeedDoc> docs) throws IOException {
        BulkRequest.Builder builder = new BulkRequest.Builder().refresh(Refresh.False);
        docs.forEach(d -> builder.operations(op -> op.index(i -> i
            .index(indexAlias)
            .id(d.id().toString())
            .document(d)
        )));
        BulkResponse response = es.bulk(builder.build());
        if (response.errors()) {
            log.warn("[FeedIndexer] upsert 실패");
            throw new IOException();
        }
        if (log.isDebugEnabled()) {
            log.debug("[FeedIndexer] upsert 완료: {}", docs.size());
        }
    }

    public void bulkDelete(List<UUID> ids) throws IOException {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        BulkRequest.Builder builder = new BulkRequest.Builder().refresh(Refresh.False);
        ids.forEach(id -> builder.operations(op -> op.delete(d -> d
            .index(indexAlias)
            .id(id.toString())
        )));

        BulkResponse response = es.bulk(builder.build());
        if (response.errors()) {
            log.warn("[FeedIndexer] delete bulk 실패");
            throw new IOException();
        }
        if (log.isDebugEnabled()) {
            log.debug("[FeedIndexer] delete 완료: {}", ids.size());
        }
    }

    public long count() throws IOException {
        return es.count(c -> c.index(indexAlias)).count();
    }

    public void refresh() throws IOException {
        es.indices().refresh(r -> r.index(indexAlias));
    }
}
