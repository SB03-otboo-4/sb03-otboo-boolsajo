package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;

public interface ClothesAttributeDefService {

    ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request);

}
