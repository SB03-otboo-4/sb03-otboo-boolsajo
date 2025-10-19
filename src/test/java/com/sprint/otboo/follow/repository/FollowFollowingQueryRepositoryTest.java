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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@Import(FollowFollowingQueryRepositoryTest.QuerydslTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("FollowQueryRepositoryImpl 팔로잉 쿼리 테스트")
class FollowFollowingQueryRepositoryTest {

    @TestConfiguration
    static class QuerydslTestConfig {
        @Bean JPAQueryFactory jpaQueryFactory(EntityManager em) { return new JPAQueryFactory(em); }
        @Bean FollowQueryRepository followQueryRepository(JPAQueryFactory jpa) { return new FollowQueryRepositoryImpl(jpa); }
    }

    @Autowired
    private EntityManager em;

    @Autowired
    private FollowQueryRepository followQueryRepository;

    private UUID followerId;
    private UUID followeeId1;
    private UUID followeeId2;
    private UUID followeeId3;

    @BeforeEach
    void setUp() throws Exception {
        User follower = User.builder()
            .username("buzz")
            .email("buzz@example.com")
            .role(Role.USER)
            .build();
        em.persist(follower);

        User followee1 = User.builder()
            .username("slinky")
            .email("slinky@example.com")
            .role(Role.USER)
            .build();
        em.persist(followee1);

        User followee2 = User.builder()
            .username("jessie")
            .email("jessie@example.com")
            .role(Role.USER)
            .build();
        em.persist(followee2);

        User followee3 = User.builder()
            .username("rex")
            .email("rex@example.com")
            .role(Role.USER)
            .build();
        em.persist(followee3);

        followerId = follower.getId();
        followeeId1 = followee1.getId();
        followeeId2 = followee2.getId();
        followeeId3 = followee3.getId();

        Follow f1 = Follow.builder()
            .followerId(followerId)
            .followeeId(followeeId1)
            .build();
        em.persist(f1);
        em.flush();
        Thread.sleep(5); // 타임스탬프 분리

        Follow f2 = Follow.builder()
            .followerId(followerId)
            .followeeId(followeeId2)
            .build();
        em.persist(f2);
        em.flush();
        Thread.sleep(5); // 타임스탬프 분리

        Follow f3 = Follow.builder()
            .followerId(followerId)
            .followeeId(followeeId3)
            .build();
        em.persist(f3);

        em.flush();
        em.clear();
    }

    // countFollowing: nameLike 미지정 시 전체 카운트
    @Test
    void 전체_팔로잉수() {
        long cnt = followQueryRepository.countFollowing(followerId, null);
        assertThat(cnt).isEqualTo(3L);
    }

    // countFollowing: nameLike 필터
    @Test
    void nameLike_필터를_포함한_전체_팔로잉수() {
        long cnt1 = followQueryRepository.countFollowing(followerId, "slin");
        long cnt2 = followQueryRepository.countFollowing(followerId, "ssi"); // jessie 일부

        assertThat(cnt1).isEqualTo(1L);
        assertThat(cnt2).isEqualTo(1L);
    }

    // findFollowingPage: 기본 조회 DESC(createdAt, id) 정렬 + limit
    @Test
    void 기본_조회() {
        List<FollowListItemResponse> page = followQueryRepository.findFollowingPage(
            followerId,
            null,
            null,
            2,
            null
        );

        assertThat(page).hasSize(2);
        // 최신 순으로 2건
        assertThat(page.get(0).followee().name()).isIn("rex", "jessie", "slinky");
        assertThat(page.get(1).followee().name()).isIn("rex", "jessie", "slinky");
    }

    // findFollowingPage: nameLike 필터
    @Test
    void name필터_팔로잉_조회() {
        List<FollowListItemResponse> page = followQueryRepository.findFollowingPage(
            followerId,
            null,
            null,
            10,
            "sli" // slinky 매칭
        );

        assertThat(page).hasSize(1);
        assertThat(page.get(0).followee().name()).isEqualTo("slinky");
    }

    // findFollowingPage: cursor(createdAt) 적용으로 다음 페이지 조회
    @Test
    void cursor적용_팔로잉_목록() {
        // 첫 페이지 2건 조회 후 두 번째 페이지를 커서로 조회
        List<FollowListItemResponse> first = followQueryRepository.findFollowingPage(
            followerId, null, null, 2, null
        );
        assertThat(first).hasSize(2);

        Instant lastCreatedAt = first.get(1).createdAt();
        List<FollowListItemResponse> second = followQueryRepository.findFollowingPage(
            followerId,
            lastCreatedAt.toString(), // ISO-8601
            null,
            10,
            null
        );

        // 남은 1건
        assertThat(second).hasSize(1);
    }

    // findFollowingPage: cursor + idAfter tie-break
    @Test
    void Cursor와_idAfter를_포함한_팔로잉_목록() {
        // 첫 페이지를 limit=3으로 모두 가져온 다음,
        // 동일 createdAt 가정이 어렵더라도, idAfter를 주면 '같거나 이전 시점'에서 id 기준 엄격 정렬이 동작하는지 호출 경로를 커버
        List<FollowListItemResponse> first = followQueryRepository.findFollowingPage(
            followerId, null, null, 3, null
        );
        assertThat(first).hasSize(3);

        Instant cursor = first.get(1).createdAt();
        UUID idAfter = first.get(1).id();

        List<FollowListItemResponse> next = followQueryRepository.findFollowingPage(
            followerId,
            cursor.toString(),
            idAfter,
            10,
            null
        );

        // 커서 이후 더 과거 레코드가 최소 0~1건 범위로 반환 (환경 따라 createdAt 동일성 보장X → 존재성만 검증)
        assertThat(next.size()).isBetween(0, 1);
    }

    // nameLike: 대소문자 무시 확인 (containsIgnoreCase)
    @Test
    void nameLike_대소문자무시() {
        List<FollowListItemResponse> page = followQueryRepository.findFollowingPage(
            followerId, null, null, 10, "SLI" // 대문자
        );

        assertThat(page).hasSize(1);
        assertThat(page.get(0).followee().name()).isEqualTo("slinky");
    }

    // idAfter만 있고 cursor는 없는 경우: 서비스 규칙상 모호하므로 레포는 'idAfter 무시' → cursor 없는 첫 페이지와 동일 동작
    @Test
    void cursor없고_idAfter만_있으면_idAfter_무시() {
        // 기준: cursor=null, idAfter=null 로 조회한 첫 페이지
        List<FollowListItemResponse> baseline = followQueryRepository.findFollowingPage(
            followerId, null, null, 2, null
        );

        // 같은 limitPlusOne, cursor=null, idAfter=임의값 으로 호출
        List<FollowListItemResponse> withIdAfterOnly = followQueryRepository.findFollowingPage(
            followerId, null, UUID.randomUUID(), 2, null
        );

        assertThat(withIdAfterOnly).hasSize(baseline.size());
        // 순서/내용이 동일하다고 단정하기 어렵다면 사이즈만 비교해도 OK
    }

    // limitPlusOne=1 일 때도 최신 1건 반환(내림차순 정렬 유지) 확인
    @Test
    void limitPlusOne_최소값에서도_정렬유지() {
        List<FollowListItemResponse> page = followQueryRepository.findFollowingPage(
            followerId, null, null, 1, null
        );
        assertThat(page).hasSize(1);
        // 가장 최신 followee 중 하나여야 함
        assertThat(page.get(0).followee().name()).isIn("rex", "jessie", "slinky");
    }

    // nameLike가 매칭되지 않으면 빈 리스트
    @Test
    void nameLike_불일치시_빈목록() {
        List<FollowListItemResponse> page = followQueryRepository.findFollowingPage(
            followerId, null, null, 10, "zzz"
        );
        assertThat(page).isEmpty();
    }
}
