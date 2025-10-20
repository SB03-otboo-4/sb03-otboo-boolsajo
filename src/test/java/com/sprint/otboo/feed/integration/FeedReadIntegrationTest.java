package com.sprint.otboo.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.fixture.WeatherLocationFixture;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = {
    "SET REFERENTIAL_INTEGRITY FALSE;",
    "TRUNCATE TABLE feed_clothes;",
    "TRUNCATE TABLE feeds;",
    "TRUNCATE TABLE clothes;",
    "TRUNCATE TABLE weathers;",
    "TRUNCATE TABLE weather_locations;",
    "TRUNCATE TABLE users;",
    "SET REFERENTIAL_INTEGRITY TRUE;"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FeedReadIntegrationTest {

    private static final String INDEX = "feed-read";

    @Container
    static final ElasticsearchContainer ES =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.5")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.http.ssl.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .waitingFor(
                Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=60s").forPort(9200)
            );

    @DynamicPropertySource
    static void esProps(DynamicPropertyRegistry r) {
        r.add("spring.elasticsearch.uris", ES::getHttpHostAddress);
        r.add("es.host", ES::getHttpHostAddress);
        r.add("spring.elasticsearch.connection-timeout", () -> "5s");
        r.add("spring.elasticsearch.socket-timeout", () -> "30s");
        r.add("spring.batch.job.enabled", () -> "false");
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ElasticsearchClient es;
    @Autowired
    ElasticsearchOperations operations;
    @Autowired
    FeedRepository feedRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WeatherLocationRepository weatherLocationRepository;
    @Autowired
    WeatherRepository weatherRepository;
    @Autowired
    FeedMapper feedMapper;

    private User author;
    private Weather weather;

    private CustomUserDetails principal(UUID userId) {
        UserDto dto = new UserDto(
            userId, Instant.now(),
            "reader@example.com", "reader",
            Role.USER, LoginType.GENERAL, false
        );
        return CustomUserDetails.builder().userDto(dto).password("pw").build();
    }

    @BeforeEach
    void setupIndexAndFixtures() throws Exception {
        // given: 인덱스 초기화 및 테스트 픽스처 저장
        deleteIndexSafely(INDEX);

        String json = new String(
            Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("feeds_index.json")
            ).readAllBytes(),
            StandardCharsets.UTF_8
        );

        es.indices().create(b -> b.index(INDEX).withJson(new StringReader(json)));
        es.indices().refresh(r -> r.index(INDEX));

        author = userRepository.save(UserFixture.createUserWithDefault());
        WeatherLocation location = weatherLocationRepository.save(
            WeatherLocationFixture.createLocationWithDefault());
        weather = weatherRepository.save(WeatherFixture.createWeatherWithDefault(location));
    }

    @AfterEach
    void cleanIndexDocs() throws Exception {
        es.deleteByQuery(d -> d.index(INDEX).query(q -> q.matchAll(m -> m)));
        es.indices().refresh(r -> r.index(INDEX));
    }

    private void deleteIndexSafely(String indexOrAlias) throws Exception {
        boolean exists = es.indices().exists(ExistsRequest.of(b -> b.index(indexOrAlias))).value();
        if (!exists) {
            return;
        }
        try {
            es.indices().delete(d -> d.index(indexOrAlias));
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            GetAliasResponse alias = es.indices()
                .getAlias(GetAliasRequest.of(b -> b.name(indexOrAlias)));
            Set<String> concrete = alias.result().keySet();
            for (String idx : concrete) {
                es.indices().delete(d -> d.index(idx));
            }
        }
    }

    private Feed saveAndIndexFeed(String content, long likeCount,
        Instant createdAt, Instant updatedAt) throws Exception {
        Feed entity = Feed.builder()
            .author(author)
            .weather(weather)
            .content(content)
            .likeCount(likeCount)
            .build();
        entity = feedRepository.save(entity);

        FeedDoc base = feedMapper.toDoc(entity);
        FeedDoc doc = new FeedDoc(
            entity.getId(),
            createdAt.truncatedTo(ChronoUnit.MILLIS),
            updatedAt.truncatedTo(ChronoUnit.MILLIS),
            base.author(),
            base.weather(),
            base.ootds(),
            content,
            likeCount,
            0,
            false
        );

        operations.save(doc, IndexCoordinates.of(INDEX));
        es.indices().refresh(r -> r.index(INDEX));
        return entity;
    }

    // -------- 테스트들 --------

    @Test
    void 기본_조회_요청시_200과_페이징_정렬_메타가_포함된다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        saveAndIndexFeed("READ-1", 0L, Instant.parse("2025-09-01T00:00:00Z"),
            Instant.parse("2025-09-01T00:00:00Z"));
        saveAndIndexFeed("READ-2", 1L, Instant.parse("2025-09-02T00:00:00Z"),
            Instant.parse("2025-09-02T00:00:00Z"));

        // when
        mockMvc.perform(
                get("/api/feeds")
                    .with(user(principal(userId)))
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            // then
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"))
            .andExpect(jsonPath("$.totalCount").isNumber())
            .andExpect(jsonPath("$.hasNext").isBoolean());
    }

    @Test
    void createdAt_기준_내림차순으로_조회되고_커서가_반환된다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Feed firstFeed = saveAndIndexFeed("C-1", 0L, Instant.parse("2025-09-01T00:00:00Z"),
            Instant.parse("2025-09-01T00:00:00Z"));
        Feed secondFeed = saveAndIndexFeed("C-2", 0L,
            Instant.parse("2025-09-01T00:00:00Z").plusMillis(1),
            Instant.parse("2025-09-01T00:00:00Z").plusMillis(1));
        Feed thirdFeed = saveAndIndexFeed("C-3", 0L,
            Instant.parse("2025-09-01T00:00:00Z").plusMillis(2),
            Instant.parse("2025-09-01T00:00:00Z").plusMillis(2));

        // when
        MvcResult response = mockMvc.perform(
                get("/api/feeds")
                    .with(user(principal(userId)))
                    .param("limit", "3")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        String body = response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode result = objectMapper.readTree(body);

        assertThat(result.path("data").size()).isGreaterThanOrEqualTo(3);
        assertThat(result.path("data").get(0).path("id").asText()).isEqualTo(
            thirdFeed.getId().toString());
        assertThat(result.path("data").get(1).path("id").asText()).isEqualTo(
            secondFeed.getId().toString());
        assertThat(result.path("data").get(2).path("id").asText()).isEqualTo(
            firstFeed.getId().toString());
        assertThat(result.path("nextCursor").isMissingNode()).isFalse();
        assertThat(result.path("nextIdAfter").isMissingNode()).isFalse();
        assertThat(result.path("sortBy").asText()).isEqualTo("createdAt");
        assertThat(result.path("sortDirection").asText()).isEqualTo("DESCENDING");
    }

    @Test
    void likeCount_기준_내림차순으로_조회되고_커서가_반환된다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Feed lowFeed = saveAndIndexFeed("L-0", 0L, Instant.parse("2025-09-01T00:00:00Z"),
            Instant.parse("2025-09-01T00:00:00Z"));
        Feed midFeed = saveAndIndexFeed("L-1", 1L, Instant.parse("2025-09-02T00:00:00Z"),
            Instant.parse("2025-09-02T00:00:00Z"));
        Feed highFeed = saveAndIndexFeed("L-2", 2L, Instant.parse("2025-09-03T00:00:00Z"),
            Instant.parse("2025-09-03T00:00:00Z"));

        // when
        MvcResult response = mockMvc.perform(
                get("/api/feeds")
                    .with(user(principal(userId)))
                    .param("limit", "3")
                    .param("sortBy", "likeCount")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn();

        // then
        JsonNode result = objectMapper.readTree(response.getResponse().getContentAsString())
            .path("data");

        assertThat(result.get(0).path("id").asText()).isEqualTo(highFeed.getId().toString());
        assertThat(result.get(1).path("id").asText()).isEqualTo(midFeed.getId().toString());
        assertThat(result.get(2).path("id").asText()).isEqualTo(lowFeed.getId().toString());
        assertThat(result.get(0).path("likeCount").asLong()).isEqualTo(2L);
        assertThat(result.get(1).path("likeCount").asLong()).isEqualTo(1L);
        assertThat(result.get(2).path("likeCount").asLong()).isEqualTo(0L);
    }

    @Test
    void limit가_0이면_400을_반환한다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        mockMvc.perform(
                get("/api/feeds")
                    .with(user(principal(userId)))
                    .param("limit", "0")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            // then
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void 지원되지_않는_sortBy면_400을_반환한다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        mockMvc.perform(
                get("/api/feeds")
                    .with(user(principal(userId)))
                    .param("limit", "10")
                    .param("sortBy", "invalidField")
                    .param("sortDirection", "DESCENDING")
                    .accept(MediaType.APPLICATION_JSON)
            )
            // then
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void 지원되지_않는_sortDirection이면_400을_반환한다() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        mockMvc.perform(
                get("/api/feeds")
                    .with(user(principal(userId)))
                    .param("limit", "10")
                    .param("sortBy", "createdAt")
                    .param("sortDirection", "DOWNWARD")
                    .accept(MediaType.APPLICATION_JSON)
            )
            // then
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
