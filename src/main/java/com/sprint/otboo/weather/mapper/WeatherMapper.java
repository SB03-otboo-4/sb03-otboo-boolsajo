package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.Weather;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface WeatherMapper {

    @Mapping(source = "id",        target = "weatherId")
    @Mapping(source = "skyStatus", target = "skyStatus")
    @Mapping(target = "temperature",   expression = "java(toTemperatureDto(weather))")
    @Mapping(target = "precipitation", expression = "java(toPrecipitationDto(weather))")
    WeatherSummaryDto toFeedWeatherDto(Weather weather);

    default TemperatureDto toTemperatureDto(Weather w) {
        if (w == null) return null;
        double current   = w.getCurrentC()   != null ? w.getCurrentC()   : 0.0;
        double compared  = w.getComparedC()  != null ? w.getComparedC()  : 0.0;
        double min       = w.getMinC()       != null ? w.getMinC()       : 0.0;
        double max       = w.getMaxC()       != null ? w.getMaxC()       : 0.0;
        return new TemperatureDto(current, compared, min, max);
    }

    default PrecipitationDto toPrecipitationDto(Weather w) {
        if (w == null) return null;
        String type = w.getType() != null ? mapType(w.getType()) : null;
        return new PrecipitationDto(
            type,
            w.getAmountMm(),
            w.getProbability()
        );
    }
    default String mapType(PrecipitationType type) {
        return type.name();
    }
}
