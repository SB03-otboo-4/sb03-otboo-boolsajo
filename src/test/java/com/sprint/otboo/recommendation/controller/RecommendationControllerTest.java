package com.sprint.otboo.recommendation.controller;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.service.RecommendationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RecommendationController.class)
@DisplayName("의상 추천 API 테스트")
public class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 추천_조회_API_성공() throws Exception {
        // given: 테스트 데이터 준비
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();
        UUID attrDefId = UUID.randomUUID();

        ClothesAttributeDto attributeDto = new ClothesAttributeDto(
            attrDefId,
            "Red"
        );

        ClothesDto clothesDto = new ClothesDto(
            clothesId,
            userId,
            "반팔 티셔츠",
            "image_url",
            ClothesType.TOP,
            List.of(attributeDto)
        );

        RecommendationDto recommendationDto = new RecommendationDto(
            weatherId,
            userId,
            List.of(clothesDto)
        );

        // Mock 서비스 동작
        when(recommendationService.getRecommendation(eq(userId), eq(weatherId)))
            .thenReturn(recommendationDto);

        // when: API 호출
        mockMvc.perform(get("/api/recommendations")
                .param("userId", userId.toString())
                .param("weatherId", weatherId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            // then: 응답 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weatherId").value(weatherId.toString()))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.clothes[0].id").value(clothesId.toString()))
            .andExpect(jsonPath("$.clothes[0].name").value("반팔 티셔츠"))
            .andExpect(jsonPath("$.clothes[0].type").value("TOP"))
            .andExpect(jsonPath("$.clothes[0].attributes[0].definitionId").value(attrDefId.toString()))
            .andExpect(jsonPath("$.clothes[0].attributes[0].value").value("Red"));
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 추천_조회_API_날씨ID_누락_실패() throws Exception {
        // given: 없음 ( weatherId 누락 )
        // when: userId만 전달하고 호출
        mockMvc.perform(get("/api/recommendations")
                .param("userId", UUID.randomUUID().toString()) // userId만 전달
                .contentType(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 추천_조회_API_userId_누락_실패() throws Exception {
        // given : 없음( userId 누락 )
        // when: weatherId만 전달하고 호출
        mockMvc.perform(get("/api/recommendations")
                .param("weatherId", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void 추천_조회_API_성공_관리자() throws Exception {
        // given: 테스트 데이터 준비
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        UUID clothesId = UUID.randomUUID();
        UUID attrDefId = UUID.randomUUID();

        ClothesAttributeDto attributeDto = new ClothesAttributeDto(attrDefId, "Red");
        ClothesDto clothesDto = new ClothesDto(clothesId, userId, "반팔 티셔츠", "image_url", ClothesType.TOP, List.of(attributeDto));
        RecommendationDto recommendationDto = new RecommendationDto(weatherId, userId, List.of(clothesDto));

        when(recommendationService.getRecommendation(eq(userId), eq(weatherId)))
            .thenReturn(recommendationDto);

        // when: ADMIN 권한으로 API 호출
        mockMvc.perform(get("/api/recommendations")
                .param("weatherId", weatherId.toString())
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON))
            // then: 응답 검증
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.weatherId").value(weatherId.toString()))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.clothes[0].id").value(clothesId.toString()))
            .andExpect(jsonPath("$.clothes[0].name").value("반팔 티셔츠"))
            .andExpect(jsonPath("$.clothes[0].type").value("TOP"))
            .andExpect(jsonPath("$.clothes[0].attributes[0].definitionId").value(attrDefId.toString()))
            .andExpect(jsonPath("$.clothes[0].attributes[0].value").value("Red"));
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void 추천_조회_API_날씨ID_누락_실패_관리자() throws Exception {
        // given: weatherId 누락
        // 파라미터 없이 호출
        mockMvc.perform(get("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request
            .andExpect(status().isBadRequest());
    }
}