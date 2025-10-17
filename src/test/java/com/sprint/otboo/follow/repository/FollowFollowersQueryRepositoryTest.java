package com.sprint.otboo.follow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.follow.entity.Follow;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(FollowFollowersQueryRepositoryTest.QuerydslTestConfig.class)
@DisplayName("FollowQueryRepositoryImpl 팔로워 쿼리 테스트")
class FollowFollowersQueryRepositoryTest {

    @TestConfiguration
    static class QuerydslTestConfig {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }

        @Bean
        FollowQueryRepository followQueryRepository(JPAQueryFactory jpa) {
            return new FollowQueryRepositoryImpl(jpa);
        }
    }

    @Autowired
    private EntityManager em;

    @Qualifier("followQueryRepository")
    @Autowired
    private FollowQueryRepository repo;

    private UUID me; // followeeId
    private UUID alice, bob, charlie; // followers

    @BeforeEach
    void setUp() throws Exception {
        // me (팔로우 당하는 쪽)
        User meUser = User.builder()
            .username("me")
            .email("me@ex.com")
            .role(Role.USER)
            .build();
        em.persist(meUser);
        me = meUser.getId();

        alice = persistUser("alice");
        Thread.sleep(2);
        bob = persistUser("bob");
        Thread.sleep(2);
        charlie = persistUser("charlie");

        persistFollow(alice, me);
        Thread.sleep(2);
        persistFollow(bob, me);
        Thread.sleep(2);
        persistFollow(charlie, me);

        em.flush();
        em.clear();
    }

    private UUID persistUser(String name) {
        User u = User.builder()
            .username(name)
            .email(name + "@ex.com")
            .role(Role.USER)
            .build();
        em.persist(u);
        return u.getId();
    }

    private void persistFollow(UUID followerId, UUID followeeId) {
        Follow f = Follow.builder()
            .followerId(followerId)
            .followeeId(followeeId)
            .build();
        em.persist(f);
    }

    // 첫 페이지 limit+1, follower 요약 채워짐, followee는 null
    @Test
    @DisplayName("findFollowersPage - 첫 페이지(limit+1), followee=null, follower 요약 OK")
    void 팔로워_첫페이지() {
        List<FollowListItemResponse> rows = repo.findFollowersPage(me, null, null, 2 + 1, null);
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).follower()).isNotNull();
        assertThat(rows.get(0).followee()).isNull(); // 설계대로 null 참조
    }

    // cursor+idAfter 타이브레이커 적용
    @Test
    @DisplayName("findFollowersPage - cursor+idAfter 타이브레이커로 다음 페이지 조회")
    void 팔로워_커서_tiebreaker() {
        List<FollowListItemResponse> first = repo.findFollowersPage(me, null, null, 2 + 1, null);
        FollowListItemResponse tail = first.get(1);
        String cursor = tail.createdAt().toString();
        UUID idAfter = tail.id();

        List<FollowListItemResponse> next = repo.findFollowersPage(me, cursor, idAfter, 2 + 1, null);
        assertThat(next.size()).isBetween(0, 1);
        if (!next.isEmpty()) {
            Instant createdAt = next.get(0).createdAt();
            assertThat(createdAt).isNotNull();
        }
    }

    @Test
    @DisplayName("countFollowers - 전체/부분 일치 필터 확인")
    void 전체_부분_일치_필터_확인() {
        long all = repo.countFollowers(me, null);
        long onlyAli = repo.countFollowers(me, "ali");
        long none = repo.countFollowers(me, "zzz");

        assertThat(all).isEqualTo(3L);
        assertThat(onlyAli).isEqualTo(1L);
        assertThat(none).isEqualTo(0L);
    }

    @Test
    @DisplayName("findFollowersPage - nameLike 대소문자 무시")
    void nameLike_대소문자_무시() {
        List<FollowListItemResponse> rows = repo.findFollowersPage(me, null, null, 10, "AL");
        assertThat(rows).extracting(r -> r.follower().name()).contains("alice");
    }

    @Test
    @DisplayName("findFollowersPage - cursor 없이 idAfter만 있으면 무시(첫 페이지와 동일)")
    void cursor_없이_idAfter만_있으면_무시() {
        List<FollowListItemResponse> baseline = repo.findFollowersPage(me, null, null, 2, null);
        List<FollowListItemResponse> withIdOnly = repo.findFollowersPage(me, null, UUID.randomUUID(), 2, null);

        assertThat(withIdOnly).hasSize(baseline.size());
    }
}
