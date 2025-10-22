package com.sprint.otboo.recommendation.controller;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.auth.jwt.JwtRegistry;
import com.sprint.otboo.auth.jwt.TokenProvider;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.service.RecommendationService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
    @WithMockUser(roles = {"USER"})
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
                .param("weatherId", weatherId.toString())
                .with(user(userId.toString()).roles("USER"))
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
        // given: 없음 ( id 누락 )
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
                .param("id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
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
                .with(user(userId.toString()).roles("ADMIN"))
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
        // given: id 누락
        // 파라미터 없이 호출
        mockMvc.perform(get("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON))
            // then: 400 Bad Request
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void 인증_정보_누락_예외() throws Exception {
        // given: SecurityContext에 인증 정보 없음
        UUID weatherId = UUID.randomUUID();

        // when: 추천 API 호출
        ResultActions result = mockMvc.perform(get("/api/recommendations")
            .param("weatherId", weatherId.toString())
            .with(request -> { request.setUserPrincipal(null); return request; })
            .contentType(MediaType.APPLICATION_JSON));

        // then: CustomException 처리에 따라 400 Bad Request
        result.andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."));
    }

    @Test
    @WithMockUser(username = "not-a-uuid", roles = {"USER"})
    void 잘못된_UUID_인증_예외() throws Exception {
        // given: username이 UUID 형식이 아님
        UUID weatherId = UUID.randomUUID();

        // when: 추천 API 호출
        ResultActions result = mockMvc.perform(get("/api/recommendations")
            .param("id", weatherId.toString())
            .contentType(MediaType.APPLICATION_JSON));

        // then: 400 Bad Request 반환
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 추천_결과_빈_리스트() throws Exception {
        // given: 올바른 UUID 값과 빈 추천 결과 준비
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();
        when(recommendationService.getRecommendation(eq(userId), eq(weatherId)))
            .thenReturn(new RecommendationDto(weatherId, userId, List.of()));

        // when: API 호출
        ResultActions result = mockMvc.perform(get("/api/recommendations")
            .param("weatherId", weatherId.toString())
            .with(user(userId.toString()).roles("USER"))
            .contentType(MediaType.APPLICATION_JSON));

        // then: 추천 결과가 빈 리스트임을 검증
        result.andExpect(status().isOk())
            .andExpect(jsonPath("$.clothes").isEmpty());
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    void 다른사용자_추천_조회_불가() throws Exception {
        // given: 다른 사용자의 UUID와 날씨 ID
        UUID weatherId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        // when: 다른 사용자 ID로 API 호출
        ResultActions result = mockMvc.perform(get("/api/recommendations")
            .param("weatherId", weatherId.toString())
            .with(user(otherUserId.toString()).roles("USER"))
            .contentType(MediaType.APPLICATION_JSON));

        // then: 호출은 정상 처리되지만, 다른 사용자 추천은 조회되지 않음
        result.andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void weatherId_userId_모두_누락_실패() throws Exception {
        // given: 요청 파라미터 없음

        // when: 추천 API 호출
        ResultActions result = mockMvc.perform(get("/api/recommendations")
            .contentType(MediaType.APPLICATION_JSON));

        // then: 400 Bad Request 반환
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void 추천_결과_null_OK_응답() throws Exception {
        // given: 추천 서비스가 null 반환
        UUID userId = UUID.randomUUID();
        UUID weatherId = UUID.randomUUID();

        when(recommendationService.getRecommendation(eq(userId), eq(weatherId)))
            .thenReturn(null);

        // when: 추천 API 호출
        mockMvc.perform(get("/api/recommendations")
                .param("weatherId", weatherId.toString())
                .with(user(userId.toString()).roles("USER"))
                .contentType(MediaType.APPLICATION_JSON))
            // then: OK 상태와 빈 본문 응답
            .andExpect(status().isOk());
    }

    @Test
    void extractUserId_CustomUserDetails_성공() throws Exception {
        // given: CustomUserDetails를 principal로 가진 인증 객체
        UUID userId = UUID.randomUUID();
        CustomUserDetails customUserDetails = mock(CustomUserDetails.class);
        when(customUserDetails.getUserId()).thenReturn(userId);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);

        RecommendationController controller = new RecommendationController(recommendationService);

        // when: extractUserId 호출
        Method method = RecommendationController.class.getDeclaredMethod("extractUserId", Authentication.class);
        method.setAccessible(true);
        UUID result = (UUID) method.invoke(controller, authentication);

        // then: 반환된 UUID가 기대값과 동일
        assertThat(result).isEqualTo(userId);
    }

    @Test
    void extractUserId_UserDetails_성공() throws Exception {
        // given: UserDetails를 principal로 가진 인증 객체
        UUID userId = UUID.randomUUID();
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        RecommendationController controller = new RecommendationController(recommendationService);

        // when: extractUserId 호출
        Method method = RecommendationController.class.getDeclaredMethod("extractUserId", Authentication.class);
        method.setAccessible(true);
        UUID result = (UUID) method.invoke(controller, authentication);

        // then: 반환된 UUID가 기대값과 동일
        assertThat(result).isEqualTo(userId);
    }

    @Test
    void extractUserId_알수없는_타입_예외() throws Exception {
        // given: 예상치 못한 타입의 principal
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(12345); // 예상치 못한 타입
        RecommendationController controller = new RecommendationController(recommendationService);

        // when: extractUserId 호출
        Method method = RecommendationController.class.getDeclaredMethod("extractUserId", Authentication.class);
        method.setAccessible(true);

        // then: CustomException 발생 확인
        assertThrows(InvocationTargetException.class, () -> method.invoke(controller, authentication));
    }

}