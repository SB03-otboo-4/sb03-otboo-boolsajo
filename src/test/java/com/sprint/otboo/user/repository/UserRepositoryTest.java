package com.sprint.otboo.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@DisplayName("UserRepository 테스트")
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

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
}
