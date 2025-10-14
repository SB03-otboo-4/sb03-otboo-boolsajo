package com.sprint.otboo.clothing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.common.config.QuerydslConfig;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({ClothesRepositoryImpl.class, QuerydslConfig.class})
@DisplayName("ClothesRepositoryImpl 커스텀 쿼리 테스트")
public class ClothesRepositoryTest {

    @Autowired
    private ClothesRepository clothesRepository;

    @Autowired
    private ClothesRepositoryImpl clothesRepositoryImpl;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        // User 엔티티 생성 및 저장
        User testUser = userRepository.saveAndFlush(
            User.builder()
                .username("testuser")
                .password("password")
                .email("testuser@example.com")
                .role(Role.USER)
                .provider(LoginType.GENERAL)
                .locked(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()
        );
        userId = testUser.getId();

        // Clothes 엔티티 생성 및 저장 (하루 단위로 createdAt 지정)
        List<Clothes> clothesList = List.of(
            Clothes.create(testUser, "Top 1", "top1.jpg", ClothesType.TOP,
                Instant.parse("2025-01-01T10:00:00Z")),
            Clothes.create(testUser, "Top 2", "top2.jpg", ClothesType.TOP,
                Instant.parse("2025-01-02T10:00:00Z")),
            Clothes.create(testUser, "Bottom 1", "bottom1.jpg", ClothesType.BOTTOM,
                Instant.parse("2025-01-03T10:00:00Z"))
        );

        clothesRepository.saveAll(clothesList);
    }

    @Test
    void 사용자_의상_조회_특정타입_커서있음() {
        // given: 특정 타입과 커서
        Instant cursor = Instant.parse("2025-01-03T10:00:00Z");
        UUID idAfter = clothesRepository.findByUser_Id(userId).get(2).getId();

        // when: 의상 조회
        List<Clothes> result = clothesRepositoryImpl.findClothesByOwner(userId, ClothesType.BOTTOM, cursor, idAfter, 5);

        // then: 결과 검증
        assertThat(result).isEmpty();
    }

    @Test
    void 사용자_의상_총개수_조회() {
        // given: 사용자 ID
        // UUID userId = this.userId;

        // when: 의상 개수 조회
        long countAll = clothesRepositoryImpl.countByOwner(userId, null);
        long countTop = clothesRepositoryImpl.countByOwner(userId, ClothesType.TOP);
        long countBottom = clothesRepositoryImpl.countByOwner(userId, ClothesType.BOTTOM);

        // then: 결과 검증
        assertThat(countAll).isEqualTo(3);
        assertThat(countTop).isEqualTo(2);
        assertThat(countBottom).isEqualTo(1);
    }
}