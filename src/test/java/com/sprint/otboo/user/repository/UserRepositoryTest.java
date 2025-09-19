package com.sprint.otboo.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.common.config.QuerydslConfig;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.query.UserQueryRepository;
import com.sprint.otboo.user.repository.query.UserQueryRepositoryImpl;
import com.sprint.otboo.user.repository.query.UserSlice;
import com.sprint.otboo.user.service.support.UserListEnums;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing(dateTimeProviderRef = "testDateTimeProvider")
@DisplayName("UserRepository 테스트")
@Import({QuerydslConfig.class,UserQueryRepositoryImpl.class, UserRepositoryTest.TestAuditConfig.class})
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserQueryRepository userQueryRepository;

    @BeforeEach
    void seedForCursorList() {
        for (int i = 0; i < 5; i++) {
            User user = User.builder()
                .username("user" + i)
                .email((char)('a' + i) + "@test.com")
                .password("encodedPassword")
                .role(i % 2 == 0 ? Role.USER : Role.ADMIN)
                .provider(LoginType.GENERAL)
                .locked(i >= 3)
                .build();
            entityManager.persist(user);
        }
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 사용자_저장_성공() {
        // given
        User user = User.builder()
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .locked(false)
            .build();

        // when
        User savedUser = userRepository.save(user);
        entityManager.flush();

        // then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testUser");
        assertThat(savedUser.getEmail()).isEqualTo("test@test.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getProvider()).isEqualTo(LoginType.GENERAL);
        assertThat(savedUser.getLocked()).isFalse();
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void 이메일로_중복_확인_정상_동작() {
        // given
        User user = User.builder()
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .build();
        entityManager.persistAndFlush(user);

        // when
        boolean existsWithEmail = userRepository.existsByEmail("test@test.com");
        boolean notExistsWithEmail = userRepository.existsByEmail("notexists@test.com");

        // then
        assertThat(existsWithEmail).isTrue();
        assertThat(notExistsWithEmail).isFalse();
    }

    @Test
    void 사용자명으로_중복_확인_정상_동작() {
        // given
        User user = User.builder()
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .build();
        entityManager.persistAndFlush(user);

        // when
        boolean existsWithUsername = userRepository.existsByUsername("testUser");
        boolean notExistsWithUsername = userRepository.existsByUsername("notexists");

        // then
        assertThat(existsWithUsername).isTrue();
        assertThat(notExistsWithUsername).isFalse();
    }

    @Test
    void 이메일로_사용자_조회_정상_동작() {
        // given
        User user = User.builder()
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .build();
        User savedUser = entityManager.persistAndFlush(user);

        // when
        Optional<User> foundUser = userRepository.findByEmail("test@test.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void 사용자명으로_사용자_조회가_정상적으로_동작한다() {
        // Given
        User user = User.builder()
            .username("testUser")
            .password("encodedPassword")
            .email("test@test.com")
            .role(Role.USER)
            .provider(LoginType.GENERAL)
            .build();
        User savedUser = entityManager.persistAndFlush(user);

        // When
        Optional<User> foundUser = userRepository.findByUsername("testUser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getUsername()).isEqualTo("testUser");
    }

    @Test
    void 첫_페이지_created_DESC_limit3() {
        // given
        int limit = 3;

        // when
        UserSlice slice = userQueryRepository.findSlice(
            null,null,limit,
            UserListEnums.SortBy.CREATED_AT, UserListEnums.SortDirection.DESCENDING,
            null, null, null
        );

        // then
        assertThat(slice.rows()).hasSize(3);
        assertThat(slice.hasNext()).isTrue();
        assertThat(slice.nextCursor()).isNotBlank();
        assertThat(slice.nextIdAfter()).isNotNull();
    }

    @Test
    void email_ASC_emailLike() {
        // given

        // when
        UserSlice slice = userQueryRepository.findSlice(
            null, null, 10,
            UserListEnums.SortBy.EMAIL, UserListEnums.SortDirection.ASCENDING,
            "test", null, null
        );

        // then
        assertThat(slice.rows()).hasSize(5);
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    void role_USER_locked_false_필터() {
        // given

        // when
        UserSlice slice = userQueryRepository.findSlice(
            null, null, 10,
            UserListEnums.SortBy.CREATED_AT, UserListEnums.SortDirection.DESCENDING,
            null, Role.USER, false
        );

        // then
        assertThat(slice.rows()).allMatch(user -> user.getRole() == Role.USER && !user.getLocked());
    }

    @Test
    void cursor_두_번째_페이지() {
        // given
        UserSlice first = userQueryRepository.findSlice(
            null, null, 2,
            UserListEnums.SortBy.CREATED_AT, UserListEnums.SortDirection.DESCENDING,
            null, null, null
        );

        // when
        UserSlice second = userQueryRepository.findSlice(
            first.nextCursor(), null, 2,
            UserListEnums.SortBy.CREATED_AT, UserListEnums.SortDirection.DESCENDING,
            null, null, null
        );

        // then
        assertThat(first.rows()).hasSize(2);
        assertThat(second.rows()).hasSize(2);
        assertThat(second.hasNext()).isTrue();
        assertThat(second.rows()).noneMatch(user -> first.rows().contains(user));
    }

    @Test
    void idAfter_다음페이지() {
        // given
        UserSlice first = userQueryRepository.findSlice(
            null, null, 2,
            UserListEnums.SortBy.CREATED_AT, UserListEnums.SortDirection.DESCENDING,
            null, null, null
        );

        // when
        UserSlice second = userQueryRepository.findSlice(
            first.nextCursor(), null, 2,
            UserListEnums.SortBy.CREATED_AT, UserListEnums.SortDirection.DESCENDING,
            null, null, null
        );

        // then
        assertThat(second.rows()).isNotEmpty();
        assertThat(second.rows()).noneMatch(user -> first.rows().contains(user));
    }

    @TestConfiguration
    static class TestAuditConfig {
        private final AtomicLong seq = new AtomicLong(0);

        @Bean("testDateTimeProvider")
        public DateTimeProvider testDateTimeProvider() {
            Instant base = Instant.parse("2025-01-01T00:00:00Z");
            // persist 될 때마다 millisecond 단위로 0,1,2,... 증가시켜 유니크한 createdAt 보장
            return () -> Optional.of(base.plus(seq.getAndIncrement(), ChronoUnit.MILLIS));
        }
    }
}
