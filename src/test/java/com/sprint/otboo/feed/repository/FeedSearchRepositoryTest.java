package com.sprint.otboo.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.feedsearch.repository.FeedSearchRepositoryImpl;
import com.sprint.otboo.fixture.FeedDocFixture;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataElasticsearchTest
@Testcontainers
@ActiveProfiles("test")
class FeedSearchRepositoryTest {

    private static final String INDEX = "feed-read";

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
    ElasticsearchOperations operations;

    @Autowired
    ElasticsearchClient es;

    @Autowired
    FeedSearchRepositoryImpl repository;

    @BeforeEach
    void recreateIndexWithMappings() throws Exception {
        boolean exists = es.indices().exists(ExistsRequest.of(b -> b.index(INDEX))).value();
        if (exists) {
            es.indices().delete(d -> d.index(INDEX));
        }

        es.indices().create(c -> c
            .index(INDEX)
            .mappings(m -> m
                .properties("id", p -> p.keyword(k -> k))
                .properties("createdAt", p -> p.date(d -> d))
                .properties("updatedAt", p -> p.date(d -> d))
                .properties("likeCount", p -> p.long_(l -> l))
                .properties("content",
                    p -> p.text(t -> t.fields("ngram", f -> f.searchAsYouType(s -> s))))
                .properties("author", p -> p.object(o -> o
                    .properties("userId", pp -> pp.keyword(k -> k))
                    .properties("name", pp -> pp.text(t -> t))
                ))
                .properties("weather", p -> p.object(o -> o
                    .properties("skyStatus", pp -> pp.keyword(k -> k))
                    .properties("precipitation", pp -> pp.object(oo -> oo
                        .properties("type", ppp -> ppp.keyword(k -> k))
                    ))
                ))
            )
        );
        es.indices().refresh(r -> r.index(INDEX));
    }

    @AfterEach
    void cleanIndex() throws Exception {
        es.deleteByQuery(d -> d.index(INDEX).query(q -> q.matchAll(m -> m)));
        es.indices().refresh(r -> r.index(INDEX));
    }

    private void indexDocs(List<FeedDoc> docs) throws Exception {
        for (FeedDoc d : docs) {
            operations.save(d,
                org.springframework.data.elasticsearch.core.mapping.IndexCoordinates.of(INDEX));
        }
        es.indices().refresh(r -> r.index(INDEX));
    }

    @Test
    void 키워드_검색과_최신순_정렬이_동작한다() throws Exception {
        // given
        FeedDoc doc1 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc doc2 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc doc3 = FeedDocFixture.createWithDefault(UUID.randomUUID());

        FeedDoc a2 = new FeedDoc(
            doc1.id(), doc1.createdAt(), Instant.parse("2025-09-01T00:00:00Z"),
            doc1.author(), doc1.weather(), doc1.ootds(),
            "자켓 코디", doc1.likeCount(), doc1.commentCount(), doc1.likedByMe()
        );
        FeedDoc b2 = new FeedDoc(
            doc2.id(), doc2.createdAt(), Instant.parse("2025-09-03T00:00:00Z"),
            doc2.author(), doc2.weather(), doc2.ootds(),
            "코트와 머플러", doc2.likeCount(), doc2.commentCount(), doc2.likedByMe()
        );
        FeedDoc c2 = new FeedDoc(
            doc3.id(), doc3.createdAt(), Instant.parse("2025-09-02T00:00:00Z"),
            doc3.author(), doc3.weather(), doc3.ootds(),
            "패딩 자켓", doc3.likeCount(), doc3.commentCount(), doc3.likedByMe()
        );

        indexDocs(List.of(a2, b2, c2));

        // when
        CursorPageResponse<UUID> page = repository.searchIds(
            null, null,
            10,
            "createdAt",
            "DESCENDING",
            "자켓",
            null,
            null,
            null
        );

        // then
        assertThat(page.data()).hasSize(2);
        assertThat(page.data()).containsExactlyInAnyOrder(doc1.id(), doc3.id());
    }

    @Test
    void 작성자_필터가_적용된다() throws Exception {
        // given
        FeedDoc doc1 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc doc2 = FeedDocFixture.createWithDefault(UUID.randomUUID());

        FeedDoc d2mod = new FeedDoc(
            doc2.id(), doc2.createdAt(), doc2.updatedAt(),
            doc2.author(), doc2.weather(), doc2.ootds(),
            doc2.content(), doc2.likeCount(), doc2.commentCount(), doc2.likedByMe()
        );

        indexDocs(List.of(doc1, d2mod));

        // when
        UUID authorId = doc1.author().userId();
        CursorPageResponse<UUID> page = repository.searchIds(
            null, null,
            10,
            "createdAt",
            "DESCENDING",
            null,
            null,
            null,
            authorId
        );

        // then
        assertThat(page.data()).containsExactly(doc1.id());
    }

    @Test
    void 날씨_필터와_카운트가_동시에_동작한다() throws Exception {
        // given
        FeedDoc doc1 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc doc2 = FeedDocFixture.createWithDefault(UUID.randomUUID());

        WeatherSummaryDto wxClearNone = new WeatherSummaryDto(
            UUID.randomUUID(),
            SkyStatus.CLEAR.name(),
            new PrecipitationDto(PrecipitationType.NONE.name(), 0.0, 0.0),
            doc1.weather().temperature()
        );
        FeedDoc x2 = new FeedDoc(
            doc1.id(), doc1.createdAt(), doc1.updatedAt(),
            doc1.author(), wxClearNone, doc1.ootds(),
            doc1.content(), doc1.likeCount(), doc1.commentCount(), doc1.likedByMe()
        );

        WeatherSummaryDto wyCloudyRain = new WeatherSummaryDto(
            UUID.randomUUID(),
            SkyStatus.CLOUDY.name(),
            new PrecipitationDto(PrecipitationType.RAIN.name(), 1.0, 80.0),
            doc2.weather().temperature()
        );
        FeedDoc y2 = new FeedDoc(
            doc2.id(), doc2.createdAt(), doc2.updatedAt(),
            doc2.author(), wyCloudyRain, doc2.ootds(),
            doc2.content(), doc2.likeCount(), doc2.commentCount(), doc2.likedByMe()
        );

        indexDocs(List.of(x2, y2));

        // when
        long count = repository.countByFilters(
            null,
            SkyStatus.CLEAR,
            PrecipitationType.NONE,
            null
        );

        // then
        assertThat(count).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void search_after_커서_페이지네이션이_정상_동작한다() throws Exception {
        // given
        FeedDoc doc1 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc doc2 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc doc3 = FeedDocFixture.createWithDefault(UUID.randomUUID());

        FeedDoc a2 = new FeedDoc(doc1.id(), doc1.createdAt(), Instant.parse("2025-09-01T00:00:00Z"),
            doc1.author(), doc1.weather(), doc1.ootds(), doc1.content(), 1L, doc1.commentCount(), doc1.likedByMe());
        FeedDoc b2 = new FeedDoc(doc2.id(), doc2.createdAt(), Instant.parse("2025-09-02T00:00:00Z"),
            doc2.author(), doc2.weather(), doc2.ootds(), doc2.content(), 5L, doc2.commentCount(), doc2.likedByMe());
        FeedDoc c2 = new FeedDoc(doc3.id(), doc3.createdAt(), Instant.parse("2025-09-03T00:00:00Z"),
            doc3.author(), doc3.weather(), doc3.ootds(), doc3.content(), 3L, doc3.commentCount(), doc3.likedByMe());

        indexDocs(List.of(a2, b2, c2));

        // when
        CursorPageResponse<UUID> p1 = repository.searchIds(
            null, null,
            2,
            "createdAt",
            "ASCENDING",
            null,
            null,
            null,
            null
        );

        // then
        assertThat(p1.data()).hasSize(2);
        assertThat(p1.hasNext()).isTrue();
        assertThat(p1.nextCursor()).isNotBlank();
        assertThat(p1.nextIdAfter()).isNotBlank();

        // when
        CursorPageResponse<UUID> p2 = repository.searchIds(
            p1.nextCursor(),
            UUID.fromString(p1.nextIdAfter()),
            2,
            "createdAt",
            "ASCENDING",
            null,
            null,
            null,
            null
        );

        // then
        assertThat(p2.data()).hasSize(1);
        assertThat(p2.hasNext()).isFalse();
    }
}
