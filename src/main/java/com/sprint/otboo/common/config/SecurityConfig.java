package com.sprint.otboo.common.config;

import com.sprint.otboo.auth.CustomAuthenticationEntryPoint;
import com.sprint.otboo.auth.SpaCsrfTokenRequestHandler;
import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.oauth.CustomOAuth2UserService;
import com.sprint.otboo.auth.oauth.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        CustomAuthenticationEntryPoint customAuthenticationEntryPoint
    ) throws Exception {
        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                    .ignoringRequestMatchers(
                                  "/api/auth/sign-in",      // 로그인
                                  "/api/auth/reset-password", // 비밀번호 초기화
                                  "/api/users/*/password",  // 비밀번호 변경
                                  "/api/users/*/lock",       // 계정 잠금 상태 변경
                                  "/api/users/*/role", // 권한 변경
                                  "/api/users/*/profiles" // 프로필 변경
                    )
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

                // 사용자 관련 API
                .requestMatchers("/api/users").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/password").permitAll()

                // 개발용 임시 설정
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/lock").permitAll()    // 계정 잠금
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").permitAll()    // 권한 변경
                .requestMatchers(HttpMethod.GET, "/api/users/*/profiles").permitAll()   // 프로필 조회
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/profiles").authenticated() // 프로필 변경
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll() // 업로드 된 파일

                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // 의상 관련 API
                .requestMatchers(HttpMethod.POST, "/api/clothes").hasAnyRole("USER", "ADMIN")  // 의상 등록( 공용 )
                .requestMatchers(HttpMethod.PATCH, "/api/clothes/{clothesId}").hasAnyRole("USER", "ADMIN") // 의상 수정( 공용 )
                .requestMatchers(HttpMethod.DELETE, "/api/clothes/{clothesId}").hasAnyRole("USER", "ADMIN") // 의상 삭제( 공용 )

                .requestMatchers(HttpMethod.POST, "/api/clothes/attribute-defs").hasRole("ADMIN")  // 의상 속성 등록( ADMIN )
                .requestMatchers(HttpMethod.PATCH, "/api/clothes/attribute-defs/**").hasRole("ADMIN") // 의상 속성 수정( ADMIN )
                .requestMatchers(HttpMethod.DELETE, "/api/clothes/attribute-defs/{definitionId}").hasRole("ADMIN") // 의상 속성 삭제( ADMIN )

                // 나머지 인증 필요
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny())
            )
            .formLogin(form ->form.disable()
            )
            .httpBasic(basic ->basic.disable())
            .exceptionHandling(handler -> handler
                .authenticationEntryPoint(customAuthenticationEntryPoint)
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
