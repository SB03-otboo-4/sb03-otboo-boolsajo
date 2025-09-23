package com.sprint.otboo.feed.repository;

import static java.time.temporal.ChronoUnit.MICROS;
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
import java.time.temporal.ChronoUnit;
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

    private static final Comparator<UUID> DB_UUID_DESC = (u1, u2) -> {
        int c = Long.compareUnsigned(u2.getMostSignificantBits(), u1.getMostSignificantBits());
        if (c != 0) {
            return c;
        }
        return Long.compareUnsigned(u2.getLeastSignificantBits(), u1.getLeastSignificantBits());
    };

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
            .thenComparing(Comment::getId, DB_UUID_DESC);

        assertThat(rows)
            .hasSize(2)
            .isSortedAccordingTo(cmp);
    }

    @Test
    void 같은_createdAt에서는_id가_타이브레이커로_동작한다() {
        // Given
        Instant fixed = Instant.now().truncatedTo(MICROS);

        Comment a = Comment.builder()
            .feed(feed).author(author).content("A").build();
        Comment b = Comment.builder()
            .feed(feed).author(author).content("B").build();

        em.persist(a);
        em.persist(b);
        em.flush();

        int updated = em.getEntityManager().createQuery("""
                update Comment c
                set c.createdAt = :t
                where c.id in :ids
                """)
            .setParameter("t", fixed)
            .setParameter("ids", List.of(a.getId(), b.getId()))
            .executeUpdate();
        assertThat(updated).isEqualTo(2);

        em.clear();

        // When: 정렬 명시 조회
        List<Comment> rows = em.getEntityManager().createQuery("""
                select c
                from Comment c
                where c.feed.id = :feedId
                order by c.createdAt desc, c.id desc
                """, Comment.class)
            .setParameter("feedId", feed.getId())
            .setMaxResults(10)
            .getResultList();

        // Then
        assertThat(rows).hasSizeGreaterThanOrEqualTo(2);

        Comment first = rows.get(0);
        Comment second = rows.get(1);

        assertThat(first.getCreatedAt().truncatedTo(MICROS))
            .isEqualTo(second.getCreatedAt().truncatedTo(MICROS));

        String firstId = first.getId().toString();
        String secondId = second.getId().toString();
        assertThat(firstId.compareTo(secondId)).isGreaterThanOrEqualTo(0);
    }
}
