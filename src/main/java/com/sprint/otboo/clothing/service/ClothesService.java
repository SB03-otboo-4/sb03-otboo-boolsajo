package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.dto.request.ClothesUpdateRequest;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.common.dto.CursorPageResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

    ClothesDto createClothes(ClothesCreateRequest request, MultipartFile image, String externalImageUrl);

    CursorPageResponse<ClothesDto> getClothesList(UUID ownerId, int limit, Instant cursor, UUID idAfter, ClothesType type);

    ClothesDto updateClothes(UUID clothesId, ClothesUpdateRequest request, MultipartFile file);

    void deleteClothes(UUID clothesId);

}
