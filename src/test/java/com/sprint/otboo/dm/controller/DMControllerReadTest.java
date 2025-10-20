package com.sprint.otboo.dm.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.Role;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = DMController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.sprint.otboo.auth.*")
    }
)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class})
@DisplayName("DM 목록 조회 컨트롤러 테스트")
class DMControllerReadTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DMService dmService;

    /* ---------- 테스트용 Principal 유틸 ---------- */
    static class TestPrincipal {
        private final UUID id;
        TestPrincipal(UUID id) { this.id = id; }
        public UUID getId() { return id; }
    }

    static class TestPrincipalStringId {
        private final String id;
        TestPrincipalStringId(String id) { this.id = id; }
        public String getId() { return id; }
    }

    static class NoGetId { }

    private static CustomUserDetails cud(UUID id, String email, String name, Role role, boolean locked) {
        UserDto dto = new UserDto(
            id,
            Instant.now(),    // createdAt
            email,
            name,
            role,
            null,             // linkedOAuthProviders
            locked
        );
        return CustomUserDetails.builder()
            .userDto(dto)
            .password("pass")
            .build();
    }

    private void setAuthenticatedUser(Object principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal, "N/A", Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        reset(dmService);
    }

    /* ---------- 성공 케이스 ---------- */

    @Test
    void dm_목록_조회_성공_200() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        setAuthenticatedUser(cud(me, "me@otboo.dev", "buzz", Role.USER, false));

        DirectMessageDto item1 = new DirectMessageDto(
            UUID.randomUUID(),
            me, "buzz", "https://s3.../buzz.png",
            other, "slinky", null,
            "안녕!", Instant.parse("2025-10-14T05:29:40Z")
        );
        DirectMessageDto item2 = new DirectMessageDto(
            UUID.randomUUID(),
            other, "slinky", null,
            me, "buzz", "https://s3.../buzz.png",
            "반가워", Instant.parse("2025-10-14T05:28:40Z")
        );

        CursorPageResponse<DirectMessageDto> resp =
            new CursorPageResponse<>(List.of(item1, item2), null, null, false, 2L, "createdAt", "DESCENDING");

        when(dmService.getDms(eq(me), eq(other), isNull(), isNull(), anyInt()))
            .thenReturn(resp);

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("limit", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    /* ---------- 유효성/에러 분기 ---------- */

    @Test
    void dm_목록_조회_커서_형식오류_400() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        setAuthenticatedUser(cud(me, "me@otboo.dev", "me", Role.USER, false));

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("cursor", "NOT_ISO")
                .param("limit", "20"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void 자기자신과의_대화_금지_400() throws Exception {
        UUID me = UUID.randomUUID();
        setAuthenticatedUser(cud(me, "me@otboo.dev", "me", Role.USER, false));

        mvc.perform(get("/api/direct-messages")
                .param("userId", me.toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void idAfter만_있고_cursor_없음_400() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        setAuthenticatedUser(cud(me, "me@otboo.dev", "me", Role.USER, false));

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("idAfter", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void limit_범위_초과_400() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        setAuthenticatedUser(cud(me, "me@otboo.dev", "me", Role.USER, false));

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("limit", "1000"))
            .andExpect(status().isBadRequest());
    }

    /* ---------- 인증/Principal 경로 분기 ---------- */

    @Test
    void 인증없음_auth_null_401() throws Exception {
        SecurityContextHolder.clearContext();
        UUID other = UUID.randomUUID();

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 인증플래그_false_401() throws Exception {
        UUID other = UUID.randomUUID();

        Authentication unauth = new UsernamePasswordAuthenticationToken(
            new Object(), "N/A", Collections.emptyList()
        );
        ((UsernamePasswordAuthenticationToken) unauth).setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauth);

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void 리플렉션_StringUUID_OK_200() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        setAuthenticatedUser(new TestPrincipalStringId(me.toString()));

        when(dmService.getDms(eq(me), eq(other), isNull(), isNull(), anyInt()))
            .thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0L, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString()))
            .andExpect(status().isOk());
    }

    @Test
    void 리플렉션_getId_없음_401() throws Exception {
        UUID other = UUID.randomUUID();
        setAuthenticatedUser(new NoGetId());

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString()))
            .andExpect(status().isUnauthorized());
    }

    /* ---------- 정상 cursor + idAfter 경로 ---------- */

    @Test
    void 커서_및_idAfter_정상_200() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        setAuthenticatedUser(cud(me, "me@otboo.dev", "me", Role.USER, false));

        Instant boundary = Instant.parse("2025-10-14T05:29:40Z");
        UUID idAfter = UUID.randomUUID();

        when(dmService.getDms(eq(me), eq(other), eq(boundary.toString()), eq(idAfter), anyInt()))
            .thenReturn(new CursorPageResponse<>(List.of(), null, null, false, 0L, "createdAt", "DESCENDING"));

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("cursor", boundary.toString())
                .param("idAfter", idAfter.toString())
                .param("limit", "2"))
            .andExpect(status().isOk());
    }
}
