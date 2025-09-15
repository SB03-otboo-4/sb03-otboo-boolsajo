package com.sprint.otboo.common.config;

import com.sprint.otboo.auth.SpaCsrfTokenRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {
        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .sessionManagement(s ->
                    s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 정적 리소스 전체 허용
                .requestMatchers("/", "/index.html", "/*.html", "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.ico").permitAll()
                .requestMatchers("/static/**", "/assets/**", "/public/**", "/resources/**", "/webjars/**").permitAll()

                // API 문서 관련 (Swagger UI)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                // Actuator
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                .requestMatchers("/api/users").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/password").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny())
            )
            .formLogin(form ->form.disable())
            .httpBasic(basic ->basic.disable());

        return http.build();
    }
}