package com.sprint.otboo.user.dto.data;

import java.math.BigDecimal;
import java.util.List;

public record ProfileLocationDto(
    BigDecimal latitude,
    BigDecimal longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {

}
