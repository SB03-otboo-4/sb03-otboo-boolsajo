package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
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
  
  // 위치 엔티티 -> 응답 DTO 변환
    default WeatherLocationResponse toLocationResponse(WeatherLocation wl) {
        if (wl == null) {
            throw new IllegalArgumentException("위치 정보는 Null이 될 수 없습니다.");
        }
        double latitude = toDouble(wl.getLatitude());
        double longitude = toDouble(wl.getLongitude());
        int x = wl.getX() == null ? 0 : wl.getX();
        int y = wl.getY() == null ? 0 : wl.getY();
        List<String> names = splitLocationNames(wl.getLocationNames());

        return new WeatherLocationResponse(
            latitude, longitude,
            x, y,
            names
        );
    }

    static double toDouble(BigDecimal value) {
        if (value == null) {
            throw new IllegalStateException("위도/경도는 Null이 될 수 없습니다.");
        }
        return value.doubleValue();
    }

    static List<String> splitLocationNames(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        // 공백과 슬래시를 공백으로 통일하고 다중 공백은 하나로 축소
        String normalized = raw.replace('/', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.isEmpty()) return List.of();
        return Arrays.stream(normalized.split(" "))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
