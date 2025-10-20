package com.sprint.otboo.dm.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.service.DMService;
import com.sprint.otboo.common.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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

    @Test
    void dm_목록_조회_성공_200() throws Exception {
        UUID me = UUID.fromString("68e17953-f79f-4d4f-8839-b26054887d5f");
        UUID other = UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b");

        DirectMessageDto item1 = new DirectMessageDto(
            UUID.fromString("386cb145-63c6-4333-89c9-6245789c6671"),
            me, "buzz", "https://s3.../buzz.png",
            other, "slinky", null,
            "안녕!", Instant.parse("2025-10-14T05:29:40Z")
        );
        DirectMessageDto item2 = new DirectMessageDto(
            UUID.fromString("93d3247e-5628-4fe7-a6da-93611e1ff732"),
            other, "slinky", null,
            me, "buzz", "https://s3.../buzz.png",
            "반가워", Instant.parse("2025-10-14T05:28:40Z")
        );

        CursorPageResponse<DirectMessageDto> resp =
            new CursorPageResponse<>(List.of(item1, item2), null, null, false, 2L, "createdAt", "DESCENDING");

        when(dmService.getDms(
            eq(me), eq(other), isNull(), isNull(), anyInt())
        ).thenReturn(resp);

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("limit", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    void dm_목록_조회_커서_형식오류_400() throws Exception {
        UUID other = UUID.fromString("947e5ff1-508a-4f72-94b1-990e206c692b");

        mvc.perform(get("/api/direct-messages")
                .param("userId", other.toString())
                .param("cursor", "NOT_ISO")
                .param("limit", "20"))
            .andExpect(status().isBadRequest());
    }
}
