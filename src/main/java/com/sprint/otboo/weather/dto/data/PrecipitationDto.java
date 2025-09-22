package com.sprint.otboo.weather.dto.data;

public record PrecipitationDto(
    String type,
    double amount,
    double probability
) {

}
