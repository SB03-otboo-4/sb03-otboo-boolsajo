package com.sprint.otboo.clothing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.service.ClothesService;
import com.sprint.otboo.common.dto.CursorPageResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
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
            .andExpect(jsonPath("$.content[0].name").value("재킷"))
            .andExpect(jsonPath("$.content[1].name").value("티셔츠"))
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
            .andExpect(jsonPath("$.content[0].type").value("TOP"))
            .andExpect(jsonPath("$.content[0].name").value("티셔츠"))
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
            .andExpect(jsonPath("$.content[0].name").value("재킷"))
            .andExpect(jsonPath("$.content[1].name").value("티셔츠"))
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
            .andExpect(jsonPath("$.content[0].type").value("TOP"))
            .andExpect(jsonPath("$.content[0].name").value("티셔츠"))
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

}