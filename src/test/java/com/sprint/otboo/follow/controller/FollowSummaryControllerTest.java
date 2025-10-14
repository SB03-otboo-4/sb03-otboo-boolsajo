package com.sprint.otboo.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.follow.dto.data.FollowSummaryDto;
import com.sprint.otboo.follow.service.FollowService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FollowController.class)
@DisplayName("팔로우 요약 정보 API")
class FollowSummaryControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean FollowService followService;
    @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean TokenProvider tokenProvider;

    // 리플렉션 분기 테스트용 프린시펄 더블
    static class PrincipalWithId {
        private final UUID id;
        PrincipalWithId(UUID id) { this.id = id; }
        public UUID getId() { return id; }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 기본 성공: name=UUID (@WithMockUser)
    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void 팔로우_요약_정보_반환() throws Exception {
        given(followService.getMySummary(any(UUID.class)))
            .willReturn(new FollowSummaryDto(3, 5));

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(3))
            .andExpect(jsonPath("$.followingCount").value(5));
    }

    // 인증 없음 → 401
    @Test
    @DisplayName("요약 조회 - 인증 없음이면 401")
    void 요약_인증없음_401() throws Exception {
        SecurityContextHolder.clearContext();

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    // name이 UUID 아님 → 401
    @Test
    @DisplayName("요약 조회 - Authentication name이 UUID가 아니면 401")
    void 요약_name이_UUID아님_401() throws Exception {
        TestingAuthenticationToken badAuth = new TestingAuthenticationToken("not-a-uuid", null);
        badAuth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(badAuth);

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    // JWT principal: userId 클레임에서 UUID 추출 → 200
    @Test
    @DisplayName("요약 조회 - JWT userId 클레임에서 UUID 추출 성공")
    void 요약_JWT_userId_200() throws Exception {
        given(followService.getMySummary(any(UUID.class)))
            .willReturn(new FollowSummaryDto(7, 9));

        UUID uid = UUID.randomUUID();
        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of("userId", uid.toString())
        );
        TestingAuthenticationToken auth = new TestingAuthenticationToken(jwt, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(7))
            .andExpect(jsonPath("$.followingCount").value(9));
    }

    // JWT principal: userId 없고 sub에서 UUID 추출 → 200
    @Test
    @DisplayName("요약 조회 - JWT sub에서 UUID 추출 성공")
    void 요약_JWT_sub_200() throws Exception {
        given(followService.getMySummary(any(UUID.class)))
            .willReturn(new FollowSummaryDto(1, 2));

        UUID uid = UUID.randomUUID();
        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of("sub", uid.toString())
        );
        TestingAuthenticationToken auth = new TestingAuthenticationToken(jwt, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(1))
            .andExpect(jsonPath("$.followingCount").value(2));
    }

    // 리플렉션 분기: principal.getId()로 UUID 추출 → 200
    @Test
    @DisplayName("요약 조회 - principal.getId() 리플렉션 추출 성공")
    void 요약_principal_getId_200() throws Exception {
        given(followService.getMySummary(any(UUID.class)))
            .willReturn(new FollowSummaryDto(4, 6));

        UUID uid = UUID.randomUUID();
        PrincipalWithId principal = new PrincipalWithId(uid);
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(4))
            .andExpect(jsonPath("$.followingCount").value(6));
    }
}
