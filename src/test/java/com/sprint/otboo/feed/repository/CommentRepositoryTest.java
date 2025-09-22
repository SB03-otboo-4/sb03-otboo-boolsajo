package com.sprint.otboo.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.common.config.QuerydslConfig;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.fixture.WeatherLocationFixture;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private TestEntityManager em;

    private UUID feedId;
    private User author;
    private WeatherLocation location;
    private Weather weather;
    private Feed feed;
    private Comment latest;
    private Comment older;

    @BeforeEach
    void setUp() {
        author = UserFixture.createUserWithDefault();
        em.persist(author);

        location = WeatherLocationFixture.createLocationWithDefault();
        em.persist(location);

        weather = WeatherFixture.createWeatherWithDefault(location);
        em.persist(weather);
        feed = FeedFixture.createEntity(author, weather);
        em.persist(feed);
        feedId = feed.getId();

        Instant t2 = Instant.now();
        Instant t1 = t2.minusSeconds(10);

        latest = Comment.builder()
            .feed(feed)
            .author(author)
            .content("첫 댓글")
            .createdAt(t2)
            .build();
        em.persist(latest);

        older = Comment.builder()
            .feed(feed)
            .author(author)
            .content("둘째 댓글")
            .createdAt(t1)
            .build();
        em.persist(older);

        em.flush();
        em.clear();
    }

    @Test
    void 기본_조회는_createdAt_DESCENDING으로_정렬된다() {

        // When
        List<Comment> rows = commentRepository.findByFeedId(
            feedId, null, null, 10
        );

        // Then
        Comparator<Comment> cmp = Comparator
            .comparing(Comment::getCreatedAt).reversed()
            .thenComparing(Comment::getId, Comparator.reverseOrder());

        assertThat(rows)
            .hasSize(2)
            .isSortedAccordingTo(cmp);
        assertThat(rows.get(0).getId()).isEqualTo(latest.getId());
        assertThat(rows.get(1).getId()).isEqualTo(older.getId());
    }

    @Test
    void 같은_createdAt에서는_id가_타이브레이커로_동작한다() {
        // Given
        Instant same = Instant.parse("2025-09-22T00:00:00Z");

        Comment a = Comment.builder()
            .feed(feed).author(author)
            .content("컨텐츠")
            .createdAt(same)
            .build();
        Comment b = Comment.builder()
            .feed(feed).author(author)
            .content("컨텐츠")
            .createdAt(same)
            .build();

        em.persist(a);
        em.persist(b);
        em.flush();
        em.clear();

        // When
        List<Comment> rows = commentRepository.findByFeedId(
            feedId, null, null, 10
        );

        // Then
        List<UUID> top2IdsSortedDesc = rows.subList(0, 2).stream()
            .map(Comment::getId)
            .sorted(Comparator.reverseOrder())
            .toList();

        assertThat(rows.subList(0, 2)).extracting(Comment::getId)
            .containsExactlyElementsOf(top2IdsSortedDesc);
    }
}
