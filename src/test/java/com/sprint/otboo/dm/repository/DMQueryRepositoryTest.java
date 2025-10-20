package com.sprint.otboo.dm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.entity.DM;
import com.sprint.otboo.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;


@DataJpaTest
@Import({DMQueryRepositoryTest.QuerydslTestConfig.class, DMQueryRepositoryImpl.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("DM 목록 조회 레포지토리 테스트")
class DMQueryRepositoryTest {

    @TestConfiguration
    static class QuerydslTestConfig {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }

    @PersistenceContext
    EntityManager em;

    DMQueryRepository repository;

    UUID me = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID other = UUID.fromString("22222222-2222-2222-2222-222222222222");

    // 고정 시각(내림차순 정렬 확인용)
    Instant t1 = Instant.parse("2025-10-14T05:30:00Z"); // 최신
    Instant t2 = Instant.parse("2025-10-14T05:29:00Z");
    Instant t3 = Instant.parse("2025-10-14T05:28:00Z");
    Instant t4 = Instant.parse("2025-10-14T05:27:00Z");
    Instant t5 = Instant.parse("2025-10-14T05:26:00Z"); // 가장 오래됨

    @BeforeEach
    void setUp() {
        repository = new DMQueryRepositoryImpl(new JPAQueryFactory(em));
        // 사용자 두 명 (sender/receiver 조인용)
        User uMe = User.builder()
            .id(me).email("me@otboo.dev").username("buzz").profileImageUrl("https://s3/me.png")
            .build();
        User uOther = User.builder()
            .id(other).email("other@otboo.dev").username("slinky").profileImageUrl(null)
            .build();
        em.persist(uMe);
        em.persist(uOther);

        // 메시지 5건: me<->other 왕복, createdAt을 명시해서 내림차순 정렬 검증
        // 최신이 먼저 오도록 t1 -> t5
        persistDm(me, other, "m1", t1);
        persistDm(other, me, "m2", t2);
        persistDm(me, other, "m3", t3);
        persistDm(other, me, "m4", t4);
        persistDm(me, other, "m5", t5);

        em.flush();
        em.clear();
    }

    @Transactional
    void persistDm(UUID sender, UUID receiver, String content, Instant createdAt) {
        DM dm = DM.builder()
            .senderId(sender)
            .receiverId(receiver)
            .content(content)
            .build();
        em.persist(dm);
        // createdAt을 고정값으로 맞춤 (BaseEntity에 setter 없으면 native update로 보정)
        em.createQuery("update DM d set d.createdAt = :ts where d.id = :id")
            .setParameter("ts", createdAt)
            .setParameter("id", dm.getId())
            .executeUpdate();
    }

    @Test
    void 페이지1_정렬_limitPlusOne() {
        int limitPlusOne = 3; // limit=2 가정
        List<DirectMessageDto> page = repository.findDmPageBetween(me, other, null, null, limitPlusOne);

        // 기대 순서: t1(m1) → t2(m2) → t3(m3)
        assertThat(page).hasSize(3);
        assertThat(page.get(0).content()).isEqualTo("m1");
        assertThat(page.get(1).content()).isEqualTo("m2");
        assertThat(page.get(2).content()).isEqualTo("m3");

        // 조인 필드도 매핑되는지 간단 확인
        assertThat(page.get(0).senderName()).isIn("buzz", "slinky");
        assertThat(page.get(0).receiverName()).isIn("buzz", "slinky");
    }

    @Test
    void 페이지2_커서_다음페이지() {
        // 첫 페이지를 limit=2로 나갔다고 가정하면, nextCursor=t2, nextIdAfter=해당 DM id
        // 여기서는 테스트 고정화를 위해 실제 첫 페이지를 한번 구해 cursor 재사용
        List<DirectMessageDto> first = repository.findDmPageBetween(me, other, null, null, 3); // limit=2 가정
        DirectMessageDto boundary = first.get(1); // 두 번째 아이템(m2, t2)
        String nextCursor = boundary.createdAt().toString();
        UUID nextIdAfter = boundary.id();

        // 다음 페이지(limit+1=3) 요청 → 기대: t2 이전(t3, t4, t5) 중 최신부터 3개 → m3, m4, m5
        List<DirectMessageDto> second = repository.findDmPageBetween(me, other, nextCursor, nextIdAfter, 3);

        assertThat(second).hasSize(3);
        assertThat(second.get(0).content()).isEqualTo("m3");
        assertThat(second.get(1).content()).isEqualTo("m4");
        assertThat(second.get(2).content()).isEqualTo("m5");
    }

    @Test
    void 카운트_검증() {
        long total = repository.countDmBetween(me, other);
        assertThat(total).isEqualTo(5L);
    }
}
