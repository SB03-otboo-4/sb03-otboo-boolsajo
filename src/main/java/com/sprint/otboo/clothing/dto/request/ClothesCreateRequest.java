package com.sprint.otboo.clothing.dto.request;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * 의상 생성 요청 DTO
 * <p>의상을 새로 등록할 때 필요한 정보를 담는 요청 객체</p>
 */
public record ClothesCreateRequest(
    @NotNull(message = "의상 소유자 ID는 필수입니다")
    UUID ownerId,

    @NotBlank(message = "의상명은 필수입니다")
    String name,

    @NotNull(message = "의상 타입은 필수입니다")
    ClothesType type,

    @Valid
    List<ClothesAttributeDto> attributes
) {

}
