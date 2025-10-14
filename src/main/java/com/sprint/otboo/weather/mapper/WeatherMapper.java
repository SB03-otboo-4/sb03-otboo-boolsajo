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

    // -------- full detail --------
    @Mapping(source = "id",           target = "weatherId")
    @Mapping(source = "forecastedAt", target = "forecastedAt")
    @Mapping(source = "forecastAt",   target = "forecastAt")
    @Mapping(target = "location",     expression = "java(toLocationResponse(weather.getLocation()))")
    @Mapping(source = "skyStatus",    target = "skyStatus")
    @Mapping(target = "precipitation", expression = "java(toPrecipitationDto(weather))")
    @Mapping(target = "humidity",      expression = "java(toHumidityDto(weather))")
    @Mapping(target = "temperature",   expression = "java(toTemperatureDto(weather))")
    @Mapping(target = "windSpeed",     expression = "java(toWindSpeedDto(weather))")
    WeatherDto toWeatherDto(Weather weather);

    // -------- summary (optional) --------
    @Mapping(source = "id",        target = "weatherId")
    @Mapping(source = "skyStatus", target = "skyStatus")
    @Mapping(target = "precipitation", expression = "java(toPrecipitationDto(weather))")
    @Mapping(target = "temperature",   expression = "java(toTemperatureDto(weather))")
    WeatherSummaryDto toWeatherSummaryDto(Weather weather);

    // -------- builders --------
    default TemperatureDto toTemperatureDto(Weather w) {
        if (w == null) return null;
        double current  = nz(w.getCurrentC());
        double compared = nz(w.getComparedC());
        double min      = nz(w.getMinC());
        double max      = nz(w.getMaxC());
        return new TemperatureDto(round2(current), round2(compared), round2(min), round2(max));
    }

    default PrecipitationDto toPrecipitationDto(Weather w) {
        if (w == null) return null;
        String type   = (w.getType() != null) ? mapType(w.getType()) : "NONE";
        double amount = nz(w.getAmountMm());
        // 저장소는 0~1 로 들어있다고 가정 → % 변환
        double probPct = nz(w.getProbability()) * 100.0;
        return new PrecipitationDto(type, round2(amount), round2(probPct));
    }

    default HumidityDto toHumidityDto(Weather w) {
        if (w == null) return null;
        double current  = nz(w.getCurrentPct());
        double compared = nz(w.getComparedPct());
        return new HumidityDto(round2(current), round2(compared));
    }

    default WindSpeedDto toWindSpeedDto(Weather w) {
        if (w == null) return null;
        double speed = nz(w.getSpeedMs());
        // 엔티티에 asWord가 이미 매핑되어 있으면 그대로, 없으면 속도로 분류
        String asWord = (w.getAsWord() != null) ? w.getAsWord().name() : classifyWind(speed);
        return new WindSpeedDto(round2(speed), asWord);
    }

    // -------- helpers --------
    static String mapType(PrecipitationType type) { return type.name(); }

    static String classifyWind(double speedMs) {
        // 프로토타입 기준: <4 : WEAK, <8 : MODERATE, >=8 : STRONG
        if (speedMs < 4.0) return "WEAK";
        if (speedMs < 8.0) return "MODERATE";
        return "STRONG";
    }

    static double nz(Double v) { return v == null ? 0.0 : v; }

    static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // -------- Location --------
    default WeatherLocationResponse toLocationResponse(WeatherLocation wl) {
        if (wl == null) return null;
        double latitude  = toDouble(wl.getLatitude());
        double longitude = toDouble(wl.getLongitude());
        int x = wl.getX() == null ? 0 : wl.getX();
        int y = wl.getY() == null ? 0 : wl.getY();
        List<String> names = splitLocationNames(wl.getLocationNames());
        return new WeatherLocationResponse(latitude, longitude, x, y, names);
    }

    static double toDouble(BigDecimal value) { return value == null ? 0.0 : value.doubleValue(); }

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
