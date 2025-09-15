package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.dto.request.ClothesCreateRequest;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.mapper.ClothesMapper;
import com.sprint.otboo.clothing.repository.ClothesAttributeRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClothesServiceImpl implements ClothesService {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeRepository clothesAttributeRepository;
    private final ClothesMapper clothesMapper;

    @Override
    public ClothesDto createClothes(ClothesCreateRequest request) {

        Clothes clothes = Clothes.create(request.ownerId(), request.name(), "", request.type());
        Clothes saved = clothesRepository.save(clothes);

        List<ClothesAttribute> attributes =
            (request.attributes() == null
                ? List.<ClothesAttributeDto>of()
                : request.attributes())
            .stream()
                .map(dto -> clothesMapper.toEntity(dto, saved.getId()))
                .toList();

        if (!attributes.isEmpty()) clothesAttributeRepository.saveAll(attributes);

        var attrDtos = attributes.stream()
            .map(clothesMapper::toAttrDto)
            .toList();
        ClothesDto result = clothesMapper.toDto(saved, attrDtos);

        return result;
    }
}
