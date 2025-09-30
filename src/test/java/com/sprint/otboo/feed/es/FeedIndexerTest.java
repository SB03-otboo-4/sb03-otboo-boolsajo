package com.sprint.otboo.feed.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.feedsearch.batch.FeedIndexer;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.fixture.FeedDocFixture;
import java.util.UUID;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataElasticsearchTest
@Testcontainers
@Import(FeedIndexer.class)
class FeedIndexerTest {

    @TestConfiguration
    static class EsMapperConfig {

        @Bean
        @Primary
        JacksonJsonpMapper jacksonJsonpMapper(ObjectMapper objectMapper) {
            return new JacksonJsonpMapper(objectMapper);
        }

        @Bean
        @Primary
        co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient(
            RestClient lowLevelClient,
            JacksonJsonpMapper mapper
        ) {
            RestClientTransport transport =
                new RestClientTransport(lowLevelClient, mapper);
            return new ElasticsearchClient(transport);
        }
    }

    @Container
    @ServiceConnection
    static final ElasticsearchContainer ES =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.5")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .waitingFor(
                Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=60s")
                    .forPort(9200)
                    .withStartupTimeout(Duration.ofSeconds(120))
            );

    @Autowired
    ElasticsearchClient es;

    @Autowired
    FeedIndexer indexer;

    private static final String WRITE_ALIAS = "feeds";


    @BeforeEach
    void ensureIndex() throws Exception {
        boolean exists = es.indices().exists(b -> b.index(WRITE_ALIAS)).value();
        if (!exists) {
            es.indices().create(b -> b.index(WRITE_ALIAS));
        }
    }

    @AfterEach
    void cleanupDocs() throws Exception {
        es.deleteByQuery(d -> d.index(WRITE_ALIAS).query(q -> q.matchAll(m -> m)));
        es.indices().refresh(r -> r.index(WRITE_ALIAS));
    }

    @Test
    void 같은_ID로_재색인하면_기존_문서가_덮어써진다() throws Exception {
        FeedDoc first = FeedDocFixture.createWithDefault(UUID.randomUUID());
        indexer.bulkUpsert(List.of(first));
        indexer.refresh();

        FeedDoc updated = new FeedDoc(
            first.id(),
            first.createdAt(),
            first.updatedAt(),
            first.author(),
            first.weather(),
            first.ootds(),
            "변경된 컨텐츠",
            first.likeCount(),
            first.commentCount(),
            first.likedByMe()
        );
        indexer.bulkUpsert(List.of(updated));
        indexer.refresh();

        CountResponse countAfter = es.count(c -> c.index(WRITE_ALIAS));
        assertThat(countAfter.count()).isEqualTo(1L);

        co.elastic.clients.elasticsearch.core.GetResponse<FeedDoc> get =
            es.get(g -> g.index(WRITE_ALIAS).id(String.valueOf(first.id())), FeedDoc.class);
        assertThat(get.found()).isTrue();
        assertThat(get.source().content()).isEqualTo("변경된 컨텐츠");
    }

    @Test
    void 같은_문서를_두_번_업서트해도_문서_수는_1이어야_한다() throws Exception {
        FeedDoc doc = FeedDocFixture.createWithDefault(UUID.randomUUID());

        indexer.bulkUpsert(List.of(doc));
        indexer.bulkUpsert(List.of(doc));
        indexer.refresh();

        CountResponse count = es.count(c -> c.index(WRITE_ALIAS));
        assertThat(count.count()).isEqualTo(1L);

        co.elastic.clients.elasticsearch.core.GetResponse<FeedDoc> get =
            es.get(g -> g.index(WRITE_ALIAS).id(String.valueOf(doc.id())), FeedDoc.class);
        assertThat(get.found()).isTrue();
        assertThat(get.source().id()).isEqualTo(doc.id());
    }

    @Test
    void 색인_후_문서를_다시_조회하면_내용과_ID가_기대값과_일치해야_한다() throws Exception {
        FeedDoc d1 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc d2 = FeedDocFixture.createWithDefault(UUID.randomUUID());

        indexer.bulkUpsert(List.of(d1, d2));
        indexer.refresh();

        CountResponse count = es.count(c -> c.index(WRITE_ALIAS));
        assertThat(count.count()).isEqualTo(2L);

        co.elastic.clients.elasticsearch.core.GetResponse<FeedDoc> g1 =
            es.get(g -> g.index(WRITE_ALIAS).id(String.valueOf(d1.id())), FeedDoc.class);
        assertThat(g1.found()).isTrue();
        assertThat(g1.source().content()).isNotBlank();
        assertThat(g1.source().id()).isEqualTo(d1.id());
    }
}
