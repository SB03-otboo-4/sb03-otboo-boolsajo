package com.sprint.otboo.feedsearch.batch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedIndexer {

    private static final String INDEX_ALIAS = "feeds";

    private final ElasticsearchClient es;

    public void bulkUpsert(List<FeedDoc> docs) throws IOException {
        BulkRequest.Builder builder = new BulkRequest.Builder().refresh(Refresh.False);

        for (FeedDoc d : docs) {
            BulkOperation op = BulkOperation.of(o -> o.index(i -> i
                .index(INDEX_ALIAS)
                .id(String.valueOf(d.id()))
                .document(d)));
            builder.operations(op);
        }

        BulkRequest request = builder.build();
        BulkResponse response = es.bulk(request);
        if (response.errors()) {
            throw new IOException("Bulk had errors");
        }
    }

    public void refresh() throws IOException {
        es.indices().refresh(r -> r.index(INDEX_ALIAS));
    }
}
