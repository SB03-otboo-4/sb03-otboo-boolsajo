package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface ClothesAttributeDefService {

    ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request);

    ClothesAttributeDefDto updateAttributeDef(UUID id, ClothesAttributeDefUpdateRequest request);

    List<ClothesAttributeDefDto> listAttributeDefs(String sortBy, String sortDirection, String keywordLike);

    void deleteAttributeDef(UUID id);
}
