package com.sprint.otboo.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.common.config.QuerydslConfig;
import com.sprint.otboo.feed.entity.Feed;


import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.user.entity.User;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;


import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import java.util.UUID;
import org.hibernate.query.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@Import(QuerydslConfig.class)
class FeedReadRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;
    @Autowired
    private TestEntityManager em;

    private User author;
    private Weather weather;

    @BeforeEach
    void setUp() {
        author = UserFixture.createUser();
        em.persist(author);

        weather = WeatherFixture.createWeather();
        em.persist(weather);

        em.flush();
        em.clear();
    }

    @ParameterizedTest
    @EnumSource(value = SortDirection.class, names = {"DESCENDING", "ASCENDING"})
    void createdAt으로_정렬시_방향대로_동작한다(SortDirection dir) {
        // Given
        Instant base = Instant.parse("2025-09-18T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            Feed f = FeedFixture.createFeedAt(author, weather, base.plusSeconds(i));
            em.persist(f);
        }
        em.flush();
        em.clear();

        // When
        List<Feed> result = feedRepository.searchByKeyword(
            null,
            5,
            "createdAt",
            dir.name(),
            null,
            null,
            null,
            null
        );

        // Then
        Comparator<Feed> cmp = Comparator.comparing(Feed::getCreatedAt);
        if (dir == SortDirection.DESCENDING) {
            cmp = cmp.reversed();
        }

        assertThat(result)
            .hasSize(5)
            .isSortedAccordingTo(cmp);
    }

    @ParameterizedTest
    @EnumSource(value = SortDirection.class, names = {"DESCENDING", "ASCENDING"})
    void likeCount로_정렬시_방향대로_동작한다(SortDirection dir) {
        // Given
        for (int i = 0; i < 5; i++) {
            Feed f = FeedFixture.createFeedWithLikeCount(author, weather, i);
            em.persist(f);
        }
        em.flush();
        em.clear();

        // When
        List<Feed> result = feedRepository.searchByKeyword(
            null,
            5,
            "likeCount",
            dir.name(),
            null,
            null,
            null,
            null
        );

        // Then
        Comparator<Feed> cmp = Comparator.comparing(Feed::getLikeCount);
        if (dir == SortDirection.DESCENDING) {
            cmp = cmp.reversed();
        }

        assertThat(result)
            .hasSize(5)
            .isSortedAccordingTo(cmp);
    }

    @Test
    void keywordLike로_검색시_조건에_맞는_피드만_반환한다() {
        // Given
        Instant now = Instant.now();

        Feed f1 = FeedFixture.createFeedWithContent(author, weather, now,
            "오늘의 코디 맑음");
        Feed f2 = FeedFixture.createFeedWithContent(author, weather, now,
            "비 오는 날 코디");
        Feed f3 = FeedFixture.createFeedWithContent(author, weather, now,
            "맑은날 산책 OOTD");
        Feed f4 = FeedFixture.createFeedWithContent(author, weather, now,
            "키워드없음");

        em.persist(f1);
        em.persist(f2);
        em.persist(f3);
        em.persist(f4);
        em.flush();
        em.clear();

        // When
        List<Feed> result = feedRepository.searchByKeyword(
            null,
            10,
            "createdAt",
            "DESCENDING",
            "맑",
            null,
            null,
            null
        );

        // Then
        assertThat(result)
            .hasSize(2)
            .extracting(Feed::getContent)
            .containsExactlyInAnyOrder("오늘의 코디 맑음", "맑은날 산책 OOTD");
    }

    @Test
    void skyStatus로_검색시_조건에_맞는_피드만_반환한다() {
        // Given
        Instant now = Instant.now();

        Weather matched = WeatherFixture.create(UUID.randomUUID(), SkyStatus.CLEAR,
            PrecipitationType.NONE);
        Weather other = WeatherFixture.create(UUID.randomUUID(), SkyStatus.CLOUDY,
            PrecipitationType.RAIN);

        em.persist(matched);
        em.persist(other);

        em.persist(
            FeedFixture.createEntity(UUID.randomUUID(), author, matched, "matched", now, null));
        em.persist(FeedFixture.createEntity(UUID.randomUUID(), author, other, "other", now, null));
        em.flush();
        em.clear();

        // When
        List<Feed> result = feedRepository.searchByKeyword(
            null, 10, "createdAt", "DESCENDING",
            null,
            SkyStatus.CLEAR,
            null,
            null
        );

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(feed ->
            assertThat(feed.getWeather().getSkyStatus()).isEqualTo(SkyStatus.CLEAR)
        );
    }

    @Test
    void precipitationType으로_검색시_조건에_맞는_피드만_반환한다() {
        // Given
        Instant now = Instant.now();

        Weather matched = WeatherFixture.create(UUID.randomUUID(), SkyStatus.CLEAR,
            PrecipitationType.RAIN);
        Weather other = WeatherFixture.create(UUID.randomUUID(), SkyStatus.CLOUDY,
            PrecipitationType.NONE);

        em.persist(matched);
        em.persist(other);

        em.persist(
            FeedFixture.createEntity(UUID.randomUUID(), author, matched, "matched", now, null));
        em.persist(FeedFixture.createEntity(UUID.randomUUID(), author, other, "other", now, null));
        em.flush();
        em.clear();

        // When
        List<Feed> result = feedRepository.searchByKeyword(
            null, 10, "createdAt", "DESCENDING",
            null,
            null,
            PrecipitationType.RAIN,
            null
        );

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(feed ->
            assertThat(feed.getWeather().getType()).isEqualTo(PrecipitationType.RAIN)
        );
    }

    @Test
    void authorId로_검색시_조건에_맞는_피드만_반환한다() {
        // Given
        Instant now = Instant.now();

        User matchedAuthor = UserFixture.createUser();
        em.persist(matchedAuthor);

        User otherAuthor = UserFixture.createUser();
        em.persist(otherAuthor);

        Feed f1 = FeedFixture.createEntity(UUID.randomUUID(), matchedAuthor, weather, null,
            now, null);
        Feed f2 = FeedFixture.createEntity(UUID.randomUUID(), otherAuthor, weather, null, now,
            null);

        em.persist(f1);
        em.persist(f2);
        em.flush();
        em.clear();

        // When
        List<Feed> result = feedRepository.searchByKeyword(
            null,
            10,
            "createdAt",
            "DESCENDING",
            null,
            null,
            null,
            matchedAuthor.getId()
        );

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(feed ->
            assertThat(feed.getAuthor().getId()).isEqualTo(matchedAuthor.getId())
        );
    }
}
