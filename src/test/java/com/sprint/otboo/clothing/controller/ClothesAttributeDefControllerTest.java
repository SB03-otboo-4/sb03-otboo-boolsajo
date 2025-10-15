package com.sprint.otboo.clothing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.clothing.service.ClothesAttributeDefService;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("의상 속성 정의 컨트롤러 테스트( ADMIN )")
public class ClothesAttributeDefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClothesAttributeDefService service;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    JwtRegistry jwtRegistry;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void ADMIN_권한이면_의상_속성_정의_등록_가능() throws Exception {
        // given: 유효한 요청 DTO와 서비스 반환 DTO
        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
            "사이즈", List.of("S", "M", "L")
        );
        ClothesAttributeDefDto responseDto = new ClothesAttributeDefDto(
            UUID.randomUUID(), "사이즈", List.of("S", "M", "L"), Instant.now()
        );
        when(service.createAttributeDef(any())).thenReturn(responseDto);

        // when: POST 요청 실행
        ResultActions resultActions = mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then: 상태코드 201과 응답 내용 검증
        resultActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
            .andExpect(jsonPath("$.name").value("사이즈"))
            .andExpect(jsonPath("$.selectableValues[0]").value("S"))
            .andExpect(jsonPath("$.selectableValues[1]").value("M"))
            .andExpect(jsonPath("$.selectableValues[2]").value("L"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void USER_권한이면_의상_속성_정의_등록_불가() throws Exception {
        // given: USER 권한과 유효한 요청 DTO
        var request = new ClothesAttributeDefCreateRequest("사이즈", List.of("S","M","L"));

        // when: POST 요청 실행
        ResultActions resultActions = mockMvc.perform(post("/api/clothes/attribute-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then: 상태코드 403 Forbidden 검증
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상_속성_정의_등록_성공() throws Exception {
        // given: ADMIN 권한, 요청 DTO와 서비스 반환 DTO 준비
        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
            "색상",
            List.of("빨강", "파랑")
        );
        ClothesAttributeDefDto responseDto = new ClothesAttributeDefDto(
            UUID.randomUUID(),
            "색상",
            List.of("빨강", "파랑"),
            Instant.now()
        );
        when(service.createAttributeDef(any())).thenReturn(responseDto);

        // when: POST 요청 실행
        mockMvc.perform(post("/api/clothes/attribute-defs")
                .with(csrf()) // CSRF 필수
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            // then: 상태 코드와 응답 JSON 검증
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
            .andExpect(jsonPath("$.name").value("색상"))
            .andExpect(jsonPath("$.selectableValues[0]").value("빨강"))
            .andExpect(jsonPath("$.selectableValues[1]").value("파랑"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상_속성_정의_수정_성공() throws Exception {
        // given: ADMIN 권한, 기존 ID와 수정 요청 DTO 준비
        UUID id = UUID.randomUUID();
        ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
            "사이즈",
            List.of("S", "M", "L")
        );
        ClothesAttributeDefDto responseDto = new ClothesAttributeDefDto(
            id,
            "사이즈",
            List.of("S", "M", "L"),
            Instant.now()
        );
        when(service.updateAttributeDef(eq(id), any())).thenReturn(responseDto);

        // when: PATCH 요청 실행
        mockMvc.perform(patch("/api/clothes/attribute-defs/{id}", id)
                .with(csrf()) // CSRF 필수
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            // then: 상태 코드와 응답 JSON 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.name").value("사이즈"))
            .andExpect(jsonPath("$.selectableValues[0]").value("S"))
            .andExpect(jsonPath("$.selectableValues[1]").value("M"))
            .andExpect(jsonPath("$.selectableValues[2]").value("L"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void ADMIN_권한_없는_사용자는_등록_불가() throws Exception {
        // given: USER 권한, 요청 DTO 준비
        ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
            "색상",
            List.of("빨강", "파랑")
        );

        // when: POST 요청 실행
        var perform = mockMvc.perform(post("/api/clothes/attribute-defs")
                .with(user("user").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print());

        // then: 접근 거부 상태 코드 확인
        perform.andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상속성정의목록조회_성공_ADMIN권한() throws Exception {
        // given: 조회할 DTO 준비 및 서비스 mocking
        ClothesAttributeDefDto dto1 = new ClothesAttributeDefDto(
            UUID.randomUUID(), "사이즈", List.of("S", "M", "L"), Instant.now()
        );
        ClothesAttributeDefDto dto2 = new ClothesAttributeDefDto(
            UUID.randomUUID(), "색상", List.of("빨강", "파랑"), Instant.now()
        );

        given(service.listAttributeDefs("name", "ASCENDING", null))
            .willReturn(List.of(dto1, dto2));

        // when: API 호출
        mockMvc.perform(get("/api/clothes/attribute-defs")
                .param("sortBy", "name")
                .param("sortDirection", "ASCENDING")
                .accept(MediaType.APPLICATION_JSON))
            // then: 결과 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("사이즈"))
            .andExpect(jsonPath("$[1].name").value("색상"));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상속성정의목록조회_성공_USER권한() throws Exception {
        // given: 조회할 DTO 준비 및 서비스 mocking
        ClothesAttributeDefDto dto1 = new ClothesAttributeDefDto(
            UUID.randomUUID(), "사이즈", List.of("S", "M", "L"), Instant.now()
        );
        ClothesAttributeDefDto dto2 = new ClothesAttributeDefDto(
            UUID.randomUUID(), "색상", List.of("빨강", "파랑"), Instant.now()
        );

        given(service.listAttributeDefs("name", "ASCENDING", null))
            .willReturn(List.of(dto1, dto2));

        // when: API 호출
        mockMvc.perform(get("/api/clothes/attribute-defs")
                .param("sortBy", "name")
                .param("sortDirection", "ASCENDING")
                .accept(MediaType.APPLICATION_JSON))
            // then: 결과 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("사이즈"))
            .andExpect(jsonPath("$[1].name").value("색상"));
    }

    @Test
    void 의상속성정의목록조회_실패_권한없음() throws Exception {
        // when: 인증 없이 API 호출
        mockMvc.perform(get("/api/clothes/attribute-defs")
                .param("sortBy", "name")
                .param("sortDirection", "ASCENDING")
                .accept(MediaType.APPLICATION_JSON))
            // then: 401 Unauthorized 확인
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상속성정의목록조회_실패_sortBy필수누락() throws Exception {
        // when: sortBy 없이 API 호출
        mockMvc.perform(get("/api/clothes/attribute-defs")
                .param("sortDirection", "ASCENDING")
                .accept(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request 확인
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상속성정의목록조회_실패_sortDirection필수누락() throws Exception {
        // when: sortDirection 없이 API 호출
        mockMvc.perform(get("/api/clothes/attribute-defs")
                .param("sortBy", "name")
                .accept(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request 확인
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상속성정의_삭제_성공_ADMIN() throws Exception {
        // given: 삭제할 ID와 서비스 mocking
        UUID id = UUID.randomUUID();
        doNothing().when(service).deleteAttributeDef(id);

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", id)
                .with(csrf())
            )
            // then: 204 No Content 확인
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void 의상속성정의_삭제_실패_존재하지않는ID() throws Exception {
        // given: 존재하지 않는 ID로 서비스에서 예외 발생
        UUID id = UUID.randomUUID();
        doThrow(new CustomException(ErrorCode.RESOURCE_NOT_FOUND))
            .when(service).deleteAttributeDef(id);

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", id)
                .with(csrf())
            )
            // then: 404 Not Found 확인
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상속성정의_삭제_실패_USER권한() throws Exception {
        // given: USER 권한
        UUID definitionId = UUID.randomUUID();

        doThrow(new AccessDeniedException("권한 없음"))
            .when(service).deleteAttributeDef(definitionId);

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", definitionId)
                .with(csrf())
            )
            // then: 403 Forbidden 확인
            .andExpect(status().isForbidden());
    }

    @Test
    void 의상속성정의_삭제_실패_인증없음() throws Exception {
        // given: 인증 없음

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/attribute-defs/{id}", UUID.randomUUID())
                .with(csrf()))
            // then: 401 Unauthorized 확인
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("권한 없는 사용자가 의상 목록 조회 시도하면 실패")
    void 권한없는사용자_의상목록조회_실패() throws Exception {
        // given: 로그인하지 않은 상태(권한 없음)
        UUID ownerId = UUID.randomUUID();

        // when: API 호출
        var resultActions = mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("limit", "10")
            .contentType(MediaType.APPLICATION_JSON));

        // then: 인증 실패(401) 확인
        resultActions
            .andExpect(status().isUnauthorized());
    }
}