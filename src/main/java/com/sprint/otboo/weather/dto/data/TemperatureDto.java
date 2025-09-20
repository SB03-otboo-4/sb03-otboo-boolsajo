package com.sprint.otboo.weather.dto.data;

public record TemperatureDto(
    double current,
    double comparedToDayBefore,
    double min,
    double max
) {

}
