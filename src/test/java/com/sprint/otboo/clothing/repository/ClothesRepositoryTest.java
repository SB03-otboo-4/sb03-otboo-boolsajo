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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

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
        // Bottom 1보다 이전 cursor와 작은 idAfter 사용
        Instant cursor = Instant.parse("2024-12-31T10:00:00Z");
        UUID idAfter = UUID.randomUUID();

        // when: 의상 조회
        List<Clothes> result = clothesRepositoryImpl.findClothesByOwner(
            userId, ClothesType.BOTTOM, cursor, idAfter, 5
        );

        // then: 결과는 존재하지 않아야 함
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

    @Test
    void 특정_ID_목록으로_사용자_의상_조회() {
        // given: 사용자 의상 전체에서 일부 ID 목록 준비
        List<Clothes> allClothes = clothesRepository.findByUserIdWithAttributes(userId);
        List<UUID> ids = allClothes.stream().map(Clothes::getId).limit(2).toList();

        // when: 해당 ID 목록으로 조회
        List<Clothes> result = clothesRepository.findAllByIdInAndUser_Id(ids, userId);

        // then: 조회 결과가 정확히 2개, ID와 사용자 일치
        assertThat(result).hasSize(2)
            .allMatch(c -> ids.contains(c.getId()))
            .allMatch(c -> c.getUser().getId().equals(userId));
    }

    @Test
    void 타입별_첫번째_의상_조회_존재() {
        // given: 특정 타입의 의상이 DB에 존재함 (TOP)

        // when: 타입별 첫 번째 의상 조회
        Optional<Clothes> result = clothesRepository.findFirstByType(ClothesType.TOP);

        // then: 결과 존재, 타입 일치
        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(ClothesType.TOP);
    }

    @Test
    void 타입별_첫번째_의상_조회_미존재() {
        // given: 특정 타입의 의상(HAT)이 DB에 없음

        // when: 타입별 첫 번째 의상 조회
        Optional<Clothes> result = clothesRepository.findFirstByType(ClothesType.HAT);

        // then: 결과 없음
        assertThat(result).isEmpty();
    }

    @Test
    void 사용자_의상_조회_withAttributes_fetchJoin_검증() {
        // given: 사용자 의상 조회 시 fetch join 적용

        // when: 사용자 의상 전체 조회
        List<Clothes> clothesList = clothesRepository.findByUserIdWithAttributes(userId);

        // then: 결과 존재, fetch join으로 attributes와 definition 접근 시 추가 쿼리 없음
        assertThat(clothesList).isNotEmpty();
        clothesList.forEach(c -> {
            c.getAttributes().forEach(a -> {
                assertThat(a.getDefinition()).isNotNull();
            });
        });
    }

    @Test
    void findClothesByOwner_페이징_및_정렬_검증() {
        // given: 페이징용 커서 시간, 페이지 사이즈 2
        Instant cursor = Instant.parse("2025-01-03T10:00:00Z");

        // when: 특정 사용자의 의상 조회 (페이징 + 정렬)
        List<Clothes> result = clothesRepositoryImpl.findClothesByOwner(userId, null, cursor, null, 2);

        // then: 조회 결과 2개, 생성일 기준 내림차순 정렬
        assertThat(result).hasSize(2)
            .extracting(Clothes::getCreatedAt)
            .isSortedAccordingTo(Comparator.reverseOrder());
    }
}