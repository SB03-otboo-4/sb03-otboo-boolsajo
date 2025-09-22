package com.sprint.otboo.clothing.valid;

import com.sprint.otboo.clothing.entity.ClothesType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.EnumSet;

/**
 * ClothesTypeValid 유효성 검증기
 *
 * <p>{@link ClothesType} 값인지 검사합니다.</p>
 */
public class ClothesTypeValidator implements ConstraintValidator<ClothesTypeValid, ClothesType> {

    /**
     * 값이 {@link ClothesType}에 포함되는지 검사
     *
     * @param value    검사할 값 (null 허용)
     * @param context  검증 컨텍스트
     * @return 유효한 값이면 true, 아니면 false
     */
    @Override
    public boolean isValid(ClothesType value, ConstraintValidatorContext context) {
        // null일 수도 있으므로 허용 (선택 파라미터)
        if (value == null) return true;

        // ClothesType enum에 포함되는지 확인
        return EnumSet.allOf(ClothesType.class).contains(value);
    }

}
