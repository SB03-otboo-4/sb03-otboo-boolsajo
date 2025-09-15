package com.sprint.otboo.weather.dto.data;

import java.util.UUID;

public record FeedWeatherDto(
    UUID weatherId,
    String skyStatus,
    PrecipitationDto precipitation,
    TemperatureDto temperature
) {

}
