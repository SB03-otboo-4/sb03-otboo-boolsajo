package com.sprint.otboo.clothing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("의상 속성 정의 컨트롤러 테스트( ADMIN )")
public class ClothesAttributeDefControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void ADMIN_권한이면_의상_속성_정의_등록_가능() throws Exception {
        // given: ADMIN 권한과 유효한 요청 DTO
        var request = new ClothesAttributeDefCreateRequest("사이즈", List.of("S","M","L"));

        // when: POST 요청 실행
        ResultActions resultActions = mockMvc.perform(post("/api/clothes/attribute-defs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        // then: 상태코드 201과 응답 내용 검증
        resultActions
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("사이즈"))
            .andExpect(jsonPath("$.selectableValues[0]").value("S"));
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
}