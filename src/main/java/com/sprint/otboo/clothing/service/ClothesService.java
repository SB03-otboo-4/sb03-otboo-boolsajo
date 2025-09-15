package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;

public interface ClothesService {

    ClothesDto createClothes(ClothesCreateRequest request);

}
