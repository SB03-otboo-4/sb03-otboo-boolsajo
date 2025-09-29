package com.sprint.otboo.user.config;

import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DefaultAdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner adminSeeder(
        @Value("${admin.default.password}") String defaultPassword) {
        return args -> {
            String email = "boolsajo@otboo.io";
            if (userRepository.existsByEmail(email)) {
                return; // 이미 존재시 아무 것도 안함.
            }

            User user = User.builder()
                .username("boolsajo")
                .email(email)
                .password(passwordEncoder.encode(defaultPassword))
                .role(Role.ADMIN)
                .locked(false)
                .provider(LoginType.GENERAL)
                .build();

            userRepository.save(user);
        };
    }
}
