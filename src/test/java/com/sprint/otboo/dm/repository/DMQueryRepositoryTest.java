package com.sprint.otboo.dm.repository;

import static com.sprint.otboo.user.entity.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.entity.DM;
import com.sprint.otboo.dm.repository.DMQueryRepositoryTest.QuerydslConfig;
import com.sprint.otboo.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;


@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Import({DMQueryRepositoryImpl.class, QuerydslConfig.class})
@DisplayName("DM 목록 조회 레포지토리 테스트")
class DMQueryRepositoryTest {

    @TestConfiguration
    static class QuerydslConfig {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }

    @PersistenceContext
    EntityManager em;

    @Qualifier("DMQueryRepositoryImpl")
    @Autowired
    DMQueryRepository repository;

    UUID me;
    UUID other;

    final Instant t1 = Instant.parse("2025-10-14T05:30:00Z");
    final Instant t2 = Instant.parse("2025-10-14T05:29:00Z");
    final Instant t3 = Instant.parse("2025-10-14T05:28:00Z");
    final Instant t4 = Instant.parse("2025-10-14T05:27:00Z");
    final Instant t5 = Instant.parse("2025-10-14T05:26:00Z");

    @BeforeEach
    void setUp() {
        // User는 ID 미지정 → persist 후 실제 PK 사용
        User uMe = User.builder()
            .email("me@otboo.dev")
            .username("buzz")
            .profileImageUrl("https://s3/me.png")
            .role(USER)
            .build();
        em.persist(uMe);

        User uOther = User.builder()
            .email("other@otboo.dev")
            .username("slinky")
            .profileImageUrl(null)
            .role(USER)
            .build();
        em.persist(uOther);

        me = uMe.getId();
        other = uOther.getId();

        persistDm(me, other, "m1", t1);
        persistDm(other, me, "m2", t2);
        persistDm(me, other, "m3", t3);
        persistDm(other, me, "m4", t4);
        persistDm(me, other, "m5", t5);

        em.flush();
        em.clear();
    }

    void persistDm(UUID sender, UUID receiver, String content, Instant createdAt) {
        DM dm = DM.builder()
            .senderId(sender)
            .receiverId(receiver)
            .content(content)
            .build();
        em.persist(dm);
        em.flush(); // id 확보

        em.createQuery("update DM d set d.createdAt = :ts where d.id = :id")
            .setParameter("ts", createdAt)
            .setParameter("id", dm.getId())
            .executeUpdate();
    }

    @Test
    void 페이지1_정렬_limitPlusOne() {
        List<DirectMessageDto> page = repository.findDmPageBetween(me, other, null, null, 3);

        assertThat(page).hasSize(3);
        assertThat(page.get(0).content()).isEqualTo("m1");
        assertThat(page.get(1).content()).isEqualTo("m2");
        assertThat(page.get(2).content()).isEqualTo("m3");
        assertThat(page.get(0).senderName()).isIn("buzz", "slinky");
        assertThat(page.get(0).receiverName()).isIn("buzz", "slinky");
    }

    @Test
    void 페이지2_커서_다음페이지() {
        List<DirectMessageDto> first = repository.findDmPageBetween(me, other, null, null, 3);
        DirectMessageDto boundary = first.get(1); // m2 @ t2

        List<DirectMessageDto> second = repository.findDmPageBetween(
            me, other, boundary.createdAt().toString(), boundary.id(), 3);

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
