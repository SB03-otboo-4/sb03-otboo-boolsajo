package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefUpdateRequest;
import com.sprint.otboo.user.entity.User;
import java.util.UUID;

public interface ClothesAttributeDefService {

    ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request);

    ClothesAttributeDefDto updateAttributeDef(UUID id, ClothesAttributeDefUpdateRequest request);

}
