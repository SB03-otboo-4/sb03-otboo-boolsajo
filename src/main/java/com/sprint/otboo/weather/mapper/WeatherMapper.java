package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.dto.data.HumidityDto;
import com.sprint.otboo.weather.dto.data.PrecipitationDto;
import com.sprint.otboo.weather.dto.data.TemperatureDto;
import com.sprint.otboo.weather.dto.data.WeatherDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.data.WindSpeedDto;
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

    @Mapping(source = "id",           target = "weatherId")
    @Mapping(source = "forecastedAt", target = "forecastedAt")
    @Mapping(source = "forecastAt",   target = "forecastAt")
    @Mapping(target = "location",      expression = "java(toLocationResponse(weather.getLocation()))")
    @Mapping(source = "skyStatus",    target = "skyStatus")
    @Mapping(target = "precipitation", expression = "java(toPrecipitationDto(weather))")
    @Mapping(target = "humidity",      expression = "java(toHumidityDto(weather))")
    @Mapping(target = "temperature",   expression = "java(toTemperatureDto(weather))")
    @Mapping(target = "windSpeed",     expression = "java(toWindSpeedDto(weather))")
    WeatherDto toWeatherDto(Weather weather);

    // ---------- builders ----------
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

        String type = (w.getType() != null) ? mapType(w.getType()) : "NONE";
        double amount = (w.getAmountMm() != null) ? w.getAmountMm() : 0.0;
        double probPct = (w.getProbability() != null) ? (w.getProbability() * 100.0) : 0.0;
        return new PrecipitationDto(type, amount, probPct);
    }

    default HumidityDto toHumidityDto(Weather w) {
        if (w == null) return null;
        double current  = w.getCurrentPct()  != null ? w.getCurrentPct()  : 0.0;
        double compared = w.getComparedPct() != null ? w.getComparedPct() : 0.0; // 서비스에서 계산 못 넣으면 0.0
        return new HumidityDto(current, compared);

    }

    default WindSpeedDto toWindSpeedDto(Weather w) {
        if (w == null) return null;
        double speed = w.getSpeedMs() != null ? w.getSpeedMs() : 0.0;
        String asWord = (w.getAsWord() != null) ? w.getAsWord().name() : "WEAK";
        return new WindSpeedDto(speed, asWord);
    }

    default String mapType(PrecipitationType type) {
        return type.name();
    }

    // ---------- Location ----------
    default WeatherLocationResponse toLocationResponse(WeatherLocation wl) {
        if (wl == null) return null;
        double latitude = toDouble(wl.getLatitude());
        double longitude = toDouble(wl.getLongitude());
        int x = wl.getX() == null ? 0 : wl.getX();
        int y = wl.getY() == null ? 0 : wl.getY();
        List<String> names = splitLocationNames(wl.getLocationNames());
        return new WeatherLocationResponse(latitude, longitude, x, y, names);
    }

    static double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    static List<String> splitLocationNames(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String normalized = raw.replace('/', ' ').replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) return List.of();
        return Arrays.stream(normalized.split(" "))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
