package com.sprint.otboo.user.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record ProfileLocationUpdateRequest(
    BigDecimal latitude,
    BigDecimal longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {
    public ProfileLocationUpdateRequest{
        locationNames = (locationNames == null) ? List.of() : List.copyOf(locationNames);
    }
}
