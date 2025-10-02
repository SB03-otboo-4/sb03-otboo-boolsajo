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
import java.util.Arrays;
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
                .properties("createdAt",
                    p -> p.date(d -> d))
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
        FeedDoc a = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc b = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc c = FeedDocFixture.createWithDefault(UUID.randomUUID());

        FeedDoc a2 = new FeedDoc(
            a.id(), a.createdAt(), Instant.parse("2025-09-01T00:00:00Z"),
            a.author(), a.weather(), a.ootds(),
            "자켓 코디", a.likeCount(), a.commentCount(), a.likedByMe()
        );
        FeedDoc b2 = new FeedDoc(
            b.id(), b.createdAt(), Instant.parse("2025-09-03T00:00:00Z"),
            b.author(), b.weather(), b.ootds(),
            "코트와 머플러", b.likeCount(), b.commentCount(), b.likedByMe()
        );
        FeedDoc c2 = new FeedDoc(
            c.id(), c.createdAt(), Instant.parse("2025-09-02T00:00:00Z"),
            c.author(), c.weather(), c.ootds(),
            "패딩 자켓", c.likeCount(), c.commentCount(), c.likedByMe()
        );

        indexDocs(Arrays.asList(a2, b2, c2));

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

        assertThat(page.data()).hasSize(2);
        // UUID 그대로 비교
        assertThat(page.data()).containsExactlyInAnyOrder(a.id(), c.id());
    }

    @Test
    void 작성자_필터가_적용된다() throws Exception {
        FeedDoc d1 = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc d2 = FeedDocFixture.createWithDefault(UUID.randomUUID());

        FeedDoc d2mod = new FeedDoc(
            d2.id(), d2.createdAt(), d2.updatedAt(),
            d2.author(), d2.weather(), d2.ootds(),
            d2.content(), d2.likeCount(), d2.commentCount(), d2.likedByMe()
        );

        indexDocs(Arrays.asList(d1, d2mod));

        UUID authorId = d1.author().userId();
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

        assertThat(page.data()).containsExactly(d1.id());
    }

    @Test
    void 날씨_필터와_카운트가_동시에_동작한다() throws Exception {
        FeedDoc x = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc y = FeedDocFixture.createWithDefault(UUID.randomUUID());

        WeatherSummaryDto wxClearNone = new WeatherSummaryDto(
            UUID.randomUUID(),
            SkyStatus.CLEAR.name(),
            new PrecipitationDto(PrecipitationType.NONE.name(), 0.0, 0.0),
            x.weather().temperature()
        );
        FeedDoc x2 = new FeedDoc(
            x.id(), x.createdAt(), x.updatedAt(),
            x.author(), wxClearNone, x.ootds(),
            x.content(), x.likeCount(), x.commentCount(), x.likedByMe()
        );

        WeatherSummaryDto wyCloudyRain = new WeatherSummaryDto(
            UUID.randomUUID(),
            SkyStatus.CLOUDY.name(),
            new PrecipitationDto(PrecipitationType.RAIN.name(), 1.0, 80.0),
            y.weather().temperature()
        );
        FeedDoc y2 = new FeedDoc(
            y.id(), y.createdAt(), y.updatedAt(),
            y.author(), wyCloudyRain, y.ootds(),
            y.content(), y.likeCount(), y.commentCount(), y.likedByMe()
        );

        indexDocs(List.of(x2, y2));

        long count = repository.countByFilters(
            null,
            SkyStatus.CLEAR,
            PrecipitationType.NONE,
            null
        );

        assertThat(count).isGreaterThanOrEqualTo(1L);
    }


    @Test
    void search_after_커서_페이지네이션이_정상_동작한다() throws Exception {
        FeedDoc a = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc b = FeedDocFixture.createWithDefault(UUID.randomUUID());
        FeedDoc c = FeedDocFixture.createWithDefault(UUID.randomUUID());

        FeedDoc a2 = new FeedDoc(a.id(), a.createdAt(), Instant.parse("2025-09-01T00:00:00Z"),
            a.author(), a.weather(), a.ootds(), a.content(), 1L, a.commentCount(), a.likedByMe());
        FeedDoc b2 = new FeedDoc(b.id(), b.createdAt(), Instant.parse("2025-09-02T00:00:00Z"),
            b.author(), b.weather(), b.ootds(), b.content(), 5L, b.commentCount(), b.likedByMe());
        FeedDoc c2 = new FeedDoc(c.id(), c.createdAt(), Instant.parse("2025-09-03T00:00:00Z"),
            c.author(), c.weather(), c.ootds(), c.content(), 3L, c.commentCount(), c.likedByMe());

        indexDocs(Arrays.asList(a2, b2, c2));

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
        assertThat(p1.data()).hasSize(2);
        assertThat(p1.hasNext()).isTrue();
        assertThat(p1.nextCursor()).isNotBlank();
        assertThat(p1.nextIdAfter()).isNotBlank();

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
        assertThat(p2.data()).hasSize(1);
        assertThat(p2.hasNext()).isFalse();
    }
}
