package com.sprint.otboo.clothing.valid;

import com.sprint.otboo.clothing.entity.ClothesType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 의상 타입 유효성 검증 어노테이션
 *
 * <p>{@link ClothesType} 값인지 확인하며, null은 허용됩니다.</p>
 *
 * <ul>
 *   <li>지원값: TOP, BOTTOM, OUTER, UNDERWEAR, ACCESSORY, SHOES, SOCKS, HAT, BAG, SCARF, ETC</li>
 *   <li>null 허용: true</li>
 * </ul>
 *
 */
@Documented
@Constraint(validatedBy = ClothesTypeValidator.class)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ClothesTypeValid {

    String message() default "유효하지 않은 의상 타입입니다. [TOP, BOTTOM, OUTER, UNDERWEAR, ACCESSORY, SHOES, SOCKS, HAT, BAG, SCARF, ETC] 중 하나여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
