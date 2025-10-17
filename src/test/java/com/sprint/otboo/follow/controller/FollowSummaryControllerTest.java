package com.sprint.otboo.follow.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtAuthenticationFilter;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.follow.dto.response.FollowSummaryResponse;
import com.sprint.otboo.follow.service.FollowService;
import com.sprint.otboo.user.service.UserQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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
@WebMvcTest(
    controllers = FollowController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = { JwtAuthenticationFilter.class }
        )
    }
)
@DisplayName("팔로우 요약 정보 API")
class FollowSummaryControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    @MockitoBean
    FollowService followService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    UserQueryService userQueryService;

    // 리플렉션 분기 테스트용 프린시펄 더블 (UUID 반환)
    static class PrincipalWithId {
        private final UUID id;
        PrincipalWithId(UUID id) { this.id = id; }
        public UUID getId() { return id; }
    }

    // 리플렉션 분기 테스트용 프린시펄 더블 (String(UUID) 반환)
    public static class PrincipalWithIdString {
        private final String id;
        public PrincipalWithIdString(String id) { this.id = id; }
        public String getId() { return id; } // 반드시 public
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 기본 성공: name=UUID (@WithMockUser)
    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void 팔로우_요약_정보_반환() throws Exception {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        given(followService.getSummary(eq(target), eq(viewer)))
            .willReturn(new FollowSummaryResponse(target, 3, 5, true, UUID.randomUUID(), false));

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followeeId").value(target.toString()))
            .andExpect(jsonPath("$.followerCount").value(3))
            .andExpect(jsonPath("$.followingCount").value(5))
            .andExpect(jsonPath("$.followedByMe").value(true))
            .andExpect(jsonPath("$.followingMe").value(false));
    }

    // 인증 없음 → 401
    @Test
    void 요약_인증없음_401() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/follows/summary")
                .param("userId", UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    // name이 UUID 아님 → 401
    @Test
    void 요약_name이_UUID아님_401() throws Exception {
        TestingAuthenticationToken badAuth = new TestingAuthenticationToken("not-a-uuid", null);
        badAuth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(badAuth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    // JWT principal: userId 클레임에서 UUID 추출 → 200
    @Test
    void 요약_JWT_userId_200() throws Exception {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followService.getSummary(eq(target), eq(viewer)))
            .willReturn(new FollowSummaryResponse(target, 7, 9, false, null, true));

        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of("userId", viewer.toString())
        );
        TestingAuthenticationToken auth = new TestingAuthenticationToken(jwt, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(7))
            .andExpect(jsonPath("$.followingCount").value(9))
            .andExpect(jsonPath("$.followingMe").value(true));
    }

    // JWT principal: userId 없고 sub에서 UUID 추출 → 200
    @Test
    void 요약_JWT_sub_200() throws Exception {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followService.getSummary(eq(target), eq(viewer)))
            .willReturn(new FollowSummaryResponse(target, 1, 2, false, null, false));

        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of("sub", viewer.toString())
        );
        TestingAuthenticationToken auth = new TestingAuthenticationToken(jwt, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(1))
            .andExpect(jsonPath("$.followingCount").value(2));
    }

    // 리플렉션 분기: principal.getId()로 UUID 추출 → 200
    @Test
    void 요약_principal_getId_200() throws Exception {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followService.getSummary(eq(target), eq(viewer)))
            .willReturn(new FollowSummaryResponse(target, 4, 6, true, UUID.randomUUID(), false));

        PrincipalWithId principal = new PrincipalWithId(viewer);
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(4))
            .andExpect(jsonPath("$.followingCount").value(6))
            .andExpect(jsonPath("$.followedByMe").value(true));
    }

    // 리플렉션 분기: principal.getId()가 String(UUID) → 200
    @Test
    void 요약_principal_getId_StringUuid_200() throws Exception {
        UUID target = UUID.randomUUID();
        String viewer = UUID.randomUUID().toString();

        given(followService.getSummary(eq(target), eq(UUID.fromString(viewer))))
            .willReturn(new FollowSummaryResponse(target, 8, 9, false, null, false));

        PrincipalWithIdString principal = new PrincipalWithIdString(viewer);
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(8))
            .andExpect(jsonPath("$.followingCount").value(9));
    }

    // JWT의 userId/sub가 모두 UUID 아님 → 401
    @Test
    void 요약_JWT_invalidClaims_401() throws Exception {
        Jwt jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            Map.of("userId", "not-uuid", "sub", "also-not-uuid")
        );
        TestingAuthenticationToken auth = new TestingAuthenticationToken(jwt, null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 요약_CustomUserDetails_200() throws Exception {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();

        given(followService.getSummary(eq(target), eq(viewer)))
            .willReturn(new FollowSummaryResponse(target, 12, 34, true, UUID.randomUUID(), true));

        CustomUserDetails cud = Mockito.mock(CustomUserDetails.class);
        Mockito.when(cud.getUserId()).thenReturn(viewer);
        Mockito.when(cud.getUsername()).thenReturn("bs8841@test.com");

        Authentication auth = new UsernamePasswordAuthenticationToken(
            cud, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(12))
            .andExpect(jsonPath("$.followingCount").value(34))
            .andExpect(jsonPath("$.followedByMe").value(true))
            .andExpect(jsonPath("$.followingMe").value(true));
    }

    @Test
    @WithMockUser(username = "4b3c2f76-3b84-4e35-9a1c-7f5a9c0d1234")
    void 요약_userId_없으면_자기자신_요약() throws Exception {
        UUID viewer = UUID.fromString("4b3c2f76-3b84-4e35-9a1c-7f5a9c0d1234");

        given(followService.getSummary(eq(viewer), eq(viewer)))
            .willReturn(new FollowSummaryResponse(viewer, 0, 0, false, null, false));

        mvc.perform(get("/api/follows/summary").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followeeId").value(viewer.toString()))
            .andExpect(jsonPath("$.followerCount").value(0))
            .andExpect(jsonPath("$.followingCount").value(0))
            .andExpect(jsonPath("$.followedByMe").value(false))
            .andExpect(jsonPath("$.followingMe").value(false));
    }

    // principal 처리 실패 후 auth.getName()(UUID 문자열) 폴백 경로
    @Test
    void 요약_auth_name_UUID_폴백() throws Exception {
        UUID target = UUID.randomUUID();
        UUID viewer = UUID.fromString("9b26e19d-63d4-4b63-8a68-7c5b9e0d1c22");

        given(followService.getSummary(eq(target), eq(viewer)))
            .willReturn(new FollowSummaryResponse(target, 2, 3, false, null, false));

        // Jwt도 아니고 getId도 없는 단순 principal. name()이 UUID가 되도록 설정.
        TestingAuthenticationToken auth = new TestingAuthenticationToken(viewer.toString(), null);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(get("/api/follows/summary")
                .param("userId", target.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.followerCount").value(2))
            .andExpect(jsonPath("$.followingCount").value(3))
            .andExpect(jsonPath("$.followedByMe").value(false))
            .andExpect(jsonPath("$.followingMe").value(false));
    }
}
