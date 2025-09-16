package com.sprint.otboo.clothing.service.impl;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDefDto;
import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.service.ClothesAttributeDefService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClothesAttributeDefServiceImpl implements ClothesAttributeDefService {

    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final ClothesMapper  clothesMapper;

    @Override
    public ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request) {

        if (request == null) {
            throw new ClothesValidationException("요청 데이터가 존재하지 않습니다");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ClothesValidationException("속성 이름은 필수입니다");
        }

        String selectValues = request.selectableValues() != null
            ? String.join(",", request.selectableValues())
            : null;

        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .name(request.name())
            .selectValues(selectValues)
            .build();

        ClothesAttributeDef saved = clothesAttributeDefRepository.save(def);

        return clothesMapper.toClothesAttributeDefDto(saved);
    }
}
