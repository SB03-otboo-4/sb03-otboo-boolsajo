package com.sprint.otboo.clothing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.service.ClothesService;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@WebMvcTest(controllers = ClothesController.class)
@DisplayName("의상 API")
public class ClothesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClothesService clothesService;

    @MockitoBean
    TokenProvider tokenProvider;

    @MockitoBean
    JwtRegistry jwtRegistry;

    @Test
    @WithMockUser(username = "testUser", roles = {"USER"})
    void 옷_등록_API_성공() throws Exception {
        // given: 의상 등록 요청 DTO와 서비스 반환 값 준비
        UUID ownerId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        var attrDto = new ClothesAttributeDto(defId, "Black");
        var request = new ClothesCreateRequest(ownerId, "화이트 티셔츠", ClothesType.TOP, List.of(attrDto));
        var response = new ClothesDto(UUID.randomUUID(), ownerId, "화이트 티셔츠", "", ClothesType.TOP, List.of(attrDto));

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
            "image",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            "dummy-image".getBytes()
        );
        when(clothesService.createClothes(any(), any())).thenReturn(response);

        // when: API 호출
        mockMvc.perform(multipart("/api/clothes")
                .file(requestPart)
                .file(imagePart)
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 응답 검증
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("화이트 티셔츠"))
            .andExpect(jsonPath("$.type").value("TOP"))
            .andExpect(jsonPath("$.attributes[0].value").value("Black"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void 옷_등록_API_관리자_권한_성공() throws Exception {
        // given: 관리자 권한으로 의상 등록 DTO와 서비스 반환 값 준비
        UUID ownerId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        var attrDto = new ClothesAttributeDto(defId, "Black");
        var request = new ClothesCreateRequest(ownerId, "화이트 티셔츠", ClothesType.TOP, List.of(attrDto));
        var response = new ClothesDto(UUID.randomUUID(), ownerId, "화이트 티셔츠", "", ClothesType.TOP, List.of(attrDto));

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
            "image",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            "dummy-image".getBytes()
        );

        when(clothesService.createClothes(any(), any())).thenReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/clothes")
                .file(requestPart)
                .file(imagePart)
                .with(csrf())  // CSRF 토큰 포함
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("화이트 티셔츠"))
            .andExpect(jsonPath("$.type").value("TOP"))
            .andExpect(jsonPath("$.attributes[0].value").value("Black"));
    }


    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 사용자_의상_목록조회_페이지네이션() throws Exception {
        // given: Service에서 반환할 의상 목록과 페이지 정보
        UUID ownerId = UUID.randomUUID();
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();

        ClothesAttributeDto attr1 = new ClothesAttributeDto(UUID.randomUUID(), "Red");
        ClothesAttributeDto attr2 = new ClothesAttributeDto(UUID.randomUUID(), "Blue");

        Instant now = Instant.now();
        Instant c2CreatedAt = now;
        Instant c1CreatedAt = now.minusSeconds(60);

        ClothesDto c1 = new ClothesDto(c1Id, ownerId, "티셔츠", null, ClothesType.TOP, List.of(attr1));
        ClothesDto c2 = new ClothesDto(c2Id, ownerId, "재킷", null, ClothesType.OUTER, List.of(attr2));

        CursorPageResponse<ClothesDto> pageResponse = new CursorPageResponse<>(
            List.of(c2, c1),
            c1CreatedAt.toString(),
            c1Id.toString(),
            false,
            2,
            "createdAt",
            "DESCENDING"
        );

        when(clothesService.getClothesList(eq(ownerId), eq(10), isNull(), isNull(), isNull()))
            .thenReturn(pageResponse);

        // when: API 호출
        var resultActions = mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("limit", "10")
            .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 검증
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("재킷"))
            .andExpect(jsonPath("$.data[1].name").value("티셔츠"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"))
            .andExpect(jsonPath("$.nextCursor").value(c1CreatedAt.toString()))
            .andExpect(jsonPath("$.nextIdAfter").value(c1Id.toString()));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 사용자_의상_목록조회_타입필터() throws Exception {
        // given: 타입 필터 TOP
        UUID ownerId = UUID.randomUUID();
        ClothesAttributeDto attr1 = new ClothesAttributeDto(UUID.randomUUID(), "Red");
        ClothesDto topClothes = new ClothesDto(UUID.randomUUID(), ownerId, "티셔츠", null, ClothesType.TOP, List.of(attr1));

        CursorPageResponse<ClothesDto> topResponse = new CursorPageResponse<>(
            List.of(topClothes),
            null,
            null,
            false,
            1,
            "createdAt",
            "DESCENDING"
        );

        when(clothesService.getClothesList(eq(ownerId), eq(10), isNull(), isNull(), eq(ClothesType.TOP)))
            .thenReturn(topResponse);

        // when: API 호출
        var resultActions = mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("limit", "10")
            .param("typeEqual", "TOP")
            .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 검증
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].type").value("TOP"))
            .andExpect(jsonPath("$.data[0].name").value("티셔츠"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void 관리자_의상_목록조회_페이지네이션() throws Exception {
        // given: Service에서 반환할 의상 목록과 페이지 정보
        UUID ownerId = UUID.randomUUID();
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();

        ClothesAttributeDto attr1 = new ClothesAttributeDto(UUID.randomUUID(), "Red");
        ClothesAttributeDto attr2 = new ClothesAttributeDto(UUID.randomUUID(), "Blue");

        Instant now = Instant.now();
        Instant c2CreatedAt = now;
        Instant c1CreatedAt = now.minusSeconds(60);

        ClothesDto c1 = new ClothesDto(c1Id, ownerId, "티셔츠", null, ClothesType.TOP, List.of(attr1));
        ClothesDto c2 = new ClothesDto(c2Id, ownerId, "재킷", null, ClothesType.OUTER, List.of(attr2));

        CursorPageResponse<ClothesDto> pageResponse = new CursorPageResponse<>(
            List.of(c2, c1),
            c1CreatedAt.toString(),
            c1Id.toString(),
            false,
            2,
            "createdAt",
            "DESCENDING"
        );

        when(clothesService.getClothesList(eq(ownerId), eq(10), isNull(), isNull(), isNull()))
            .thenReturn(pageResponse);

        // when: API 호출
        var resultActions = mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("limit", "10")
            .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 검증
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("재킷"))
            .andExpect(jsonPath("$.data[1].name").value("티셔츠"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(2))
            .andExpect(jsonPath("$.sortBy").value("createdAt"))
            .andExpect(jsonPath("$.sortDirection").value("DESCENDING"))
            .andExpect(jsonPath("$.nextCursor").value(c1CreatedAt.toString()))
            .andExpect(jsonPath("$.nextIdAfter").value(c1Id.toString()));
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void 관리자_의상_목록조회_타입필터() throws Exception {
        // given: 타입 필터 TOP
        UUID ownerId = UUID.randomUUID();
        ClothesAttributeDto attr1 = new ClothesAttributeDto(UUID.randomUUID(), "Red");
        ClothesDto topClothes = new ClothesDto(UUID.randomUUID(), ownerId, "티셔츠", null, ClothesType.TOP, List.of(attr1));

        CursorPageResponse<ClothesDto> topResponse = new CursorPageResponse<>(
            List.of(topClothes),
            null,
            null,
            false,
            1,
            "createdAt",
            "DESCENDING"
        );

        when(clothesService.getClothesList(eq(ownerId), eq(10), isNull(), isNull(), eq(ClothesType.TOP)))
            .thenReturn(topResponse);

        // when: API 호출
        var resultActions = mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("limit", "10")
            .param("typeEqual", "TOP")
            .contentType(MediaType.APPLICATION_JSON));

        // then: 응답 검증
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].type").value("TOP"))
            .andExpect(jsonPath("$.data[0].name").value("티셔츠"))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.totalCount").value(1));
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

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상목록조회_허용되지않는타입_예외() throws Exception {
        // given: 잘못된 타입 값
        UUID ownerId = UUID.randomUUID();

        // when: API 호출
        var resultActions = mockMvc.perform(get("/api/clothes")
            .param("ownerId", ownerId.toString())
            .param("limit", "10")
            .param("typeEqual", "INVALID_TYPE")  // ❌ 존재하지 않는 타입
            .contentType(MediaType.APPLICATION_JSON));

        // then: 400 상태코드와 에러 메시지 확인
        resultActions
            .andExpect(status().isBadRequest())
            .andExpect(result ->
                assertThat(result.getResolvedException())
                    .isInstanceOfAny(MethodArgumentTypeMismatchException.class, ConstraintViolationException.class)
            );
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상수정_API_USER_성공() throws Exception {
        // given: USER 권한으로 의상 수정 요청 준비
        UUID clothesId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        var attrDto = new ClothesAttributeDto(defId, "Black");
        var updateRequest = new ClothesUpdateRequest(
            "새 티셔츠",
            ClothesType.TOP,
            List.of(attrDto)
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(updateRequest)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
            "image",
            "test.png",
            MediaType.IMAGE_PNG_VALUE,
            "dummy-image".getBytes()
        );

        var response = new ClothesDto(clothesId, ownerId, "새 티셔츠", "/uploads/new.png", ClothesType.TOP, List.of(attrDto));

        when(clothesService.updateClothes(eq(clothesId), any(), any())).thenReturn(response);

        // when: API 호출
        mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
                .file(requestPart)
                .file(imagePart)
                .with(csrf())
                .with(request -> { request.setMethod("PATCH"); return request; }) // multipart에서 PATCH 지정
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 응답 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clothesId.toString()))
            .andExpect(jsonPath("$.name").value("새 티셔츠"))
            .andExpect(jsonPath("$.type").value("TOP"))
            .andExpect(jsonPath("$.attributes[0].value").value("Black"));
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void 의상수정_API_ADMIN_성공() throws Exception {
        // given: ADMIN 권한으로 의상 수정 요청 준비
        UUID clothesId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        var attrDto = new ClothesAttributeDto(defId, "White");
        var adminUpdateRequest = new ClothesUpdateRequest(
            "새 재킷",
            ClothesType.OUTER,
            List.of(attrDto)
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(adminUpdateRequest)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
            "image",
            "admin.png",
            MediaType.IMAGE_PNG_VALUE,
            "dummy-image".getBytes()
        );

        var response = new ClothesDto(clothesId, ownerId, "새 재킷", "/uploads/admin.png", ClothesType.OUTER, List.of(attrDto));

        when(clothesService.updateClothes(eq(clothesId), any(), any())).thenReturn(response);

        // when: API 호출
        mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
                .file(requestPart)
                .file(imagePart)
                .with(csrf())
                .with(request -> { request.setMethod("PATCH"); return request; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 응답 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clothesId.toString()))
            .andExpect(jsonPath("$.name").value("새 재킷"))
            .andExpect(jsonPath("$.type").value("OUTER"))
            .andExpect(jsonPath("$.attributes[0].value").value("White"));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상_수정_실패_존재하지_않는_의상() throws Exception {
        // given: 존재하지 않는 의상 ID와 요청 DTO
        UUID clothesId = UUID.randomUUID();
        var notFoundRequest = new ClothesUpdateRequest(
            "새 티셔츠", ClothesType.TOP, Collections.emptyList()
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(notFoundRequest)
        );

        when(clothesService.updateClothes(eq(clothesId), any(), any()))
            .thenThrow(new CustomException(ErrorCode.CLOTHES_NOT_FOUND));

        // when: PATCH multipart 요청
        mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
                .file(requestPart)
                .with(request -> { request.setMethod("PATCH"); return request; })
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 404 Not Found + 에러 코드/메시지 검증
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CLOTHES_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("의상 정보를 찾을 수 없습니다."));
    }

    @Test
    void 의상_수정_실패_권한_없는_사용자() throws Exception {
        // given: 요청 DTO (인증 없이)
        UUID clothesId = UUID.randomUUID();
        var unauthorizedRequest = new ClothesUpdateRequest(
            "새 티셔츠", ClothesType.TOP, Collections.emptyList()
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(unauthorizedRequest)
        );

        // when: PATCH multipart 요청 (인증 없음)
        mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
                .file(requestPart)
                .with(request -> { request.setMethod("PATCH"); return request; })
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 403 Forbidden
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상_수정_이미지_없이_이름_타입_속성만_갱신() throws Exception {
        // given: 의상 ID, 수정 요청 DTO (이미지 없음)
        UUID clothesId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();

        var updateRequest = new ClothesUpdateRequest(
            "새 상의", ClothesType.OUTER, List.of(new ClothesAttributeDto(defId, "BLUE"))
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(updateRequest)
        );

        var response = new ClothesDto(clothesId, UUID.randomUUID(), "새 상의", "old_image_url", ClothesType.OUTER,
            List.of(new ClothesAttributeDto(defId, "BLUE")));

        when(clothesService.updateClothes(eq(clothesId), any(), isNull())).thenReturn(response);

        // when: PATCH multipart 요청
        mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
                .file(requestPart)
                .with(request -> { request.setMethod("PATCH"); return request; })
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 200 OK + 필드 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("새 상의"))
            .andExpect(jsonPath("$.type").value("OUTER"))
            .andExpect(jsonPath("$.imageUrl").value("old_image_url"))
            .andExpect(jsonPath("$.attributes[0].value").value("BLUE"));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상_수정_이미지_포함() throws Exception {
        // given: 의상 ID, 수정 요청 DTO, 새 이미지
        UUID clothesId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();

        var updateRequest = new ClothesUpdateRequest(
            "새 티셔츠", ClothesType.TOP, List.of(new ClothesAttributeDto(defId, "BLACK"))
        );

        MockMultipartFile requestPart = new MockMultipartFile(
            "request", "", MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(updateRequest)
        );

        MockMultipartFile imagePart = new MockMultipartFile(
            "image", "new.png", MediaType.IMAGE_PNG_VALUE, "dummy-image".getBytes()
        );

        var response = new ClothesDto(clothesId, UUID.randomUUID(), "새 티셔츠", "/uploads/new.png", ClothesType.TOP,
            List.of(new ClothesAttributeDto(defId, "BLACK")));

        when(clothesService.updateClothes(eq(clothesId), any(), any())).thenReturn(response);

        // when: PATCH multipart 요청 (이미지 포함)
        mockMvc.perform(multipart("/api/clothes/{clothesId}", clothesId)
                .file(requestPart)
                .file(imagePart)
                .with(request -> { request.setMethod("PATCH"); return request; })
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
            // then: 200 OK + 필드 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("새 티셔츠"))
            .andExpect(jsonPath("$.type").value("TOP"))
            .andExpect(jsonPath("$.imageUrl").value("/uploads/new.png"))
            .andExpect(jsonPath("$.attributes[0].value").value("BLACK"));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상삭제_USER성공() throws Exception {
        // given: 삭제할 의상 ID와 서비스 mocking
        UUID clothesId = UUID.randomUUID();
        doNothing().when(clothesService).deleteClothes(clothesId);

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
                .with(csrf()))
            // then: 204 No Content 확인 및 서비스 호출 검증
            .andExpect(status().isNoContent());

        verify(clothesService, times(1)).deleteClothes(clothesId);
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void 의상삭제_ADMIN성공() throws Exception {
        // given: 삭제할 의상 ID와 서비스 mocking
        UUID clothesId = UUID.randomUUID();
        doNothing().when(clothesService).deleteClothes(clothesId);

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
                .with(csrf()))
            // then: 204 No Content 확인 및 서비스 호출 검증
            .andExpect(status().isNoContent());

        verify(clothesService, times(1)).deleteClothes(clothesId);
    }

    @Test
    @DisplayName("의상 삭제 실패 - 인증 없음")
    void 의상삭제_실패_인증없음() throws Exception {
        // given: 인증 없음
        UUID clothesId = UUID.randomUUID();

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId))
            // then: 403 Forbidden 확인
            .andExpect(status().isForbidden());

        verify(clothesService, never()).deleteClothes(any());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 의상삭제_실패_존재하지않음() throws Exception {
        // given: 존재하지 않는 의상 ID로 서비스에서 예외 발생
        UUID clothesId = UUID.randomUUID();
        doThrow(new CustomException(ErrorCode.CLOTHES_NOT_FOUND))
            .when(clothesService).deleteClothes(clothesId);

        // when: DELETE 요청 실행
        mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
                .with(csrf()))
            // then: 404 Not Found 확인, 에러 코드 / 메시지 검증
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CLOTHES_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("의상 정보를 찾을 수 없습니다."));

        verify(clothesService, times(1)).deleteClothes(clothesId);
    }
}