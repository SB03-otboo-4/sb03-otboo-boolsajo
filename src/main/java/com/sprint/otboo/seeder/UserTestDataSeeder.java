package com.sprint.otboo.seeder;

import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
@Order(1)
public class UserTestDataSeeder implements DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void seed() {
        userRepository.findAll().stream().findFirst().orElseGet(() -> {
            String encoded = passwordEncoder.encode("password");
            User user = userRepository.save(User.builder()
                .username("tester")
                .password(encoded)
                .email("tester@example.com")
                .role(Role.USER)
                .locked(false)
                .profileImageUrl("https://placehold.co/100x100")
                .provider(LoginType.KAKAO)
                .build());
            log.info("[UserSeeder] seeded user id={}", user.getId());
            return user;
        });
    }
}
