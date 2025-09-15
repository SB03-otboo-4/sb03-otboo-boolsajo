package com.sprint.otboo.clothing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.service.ClothesService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(ClothesController.class)
@DisplayName("의상 API")
public class ClothesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private ClothesService clothesService;

    @Test
    void 옷_등록_API_성공() throws Exception {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        var attrDto = new ClothesAttributeDto(defId, "Black");
        var request = new ClothesCreateRequest(ownerId, "화이트 티셔츠", ClothesType.TOP, List.of(attrDto));
        var response = new ClothesDto(UUID.randomUUID(), ownerId, "화이트 티셔츠", "", ClothesType.TOP, List.of(attrDto));

        when(clothesService.createClothes(any())).thenReturn(response);

        // when / then
        mockMvc.perform(post("/api/clothes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("화이트 티셔츠"))
            .andExpect(jsonPath("$.type").value("TOP"))
            .andExpect(jsonPath("$.attributes[0].value").value("Black"));
    }
}