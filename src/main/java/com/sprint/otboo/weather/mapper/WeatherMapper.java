package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class WeatherMapper {

    public static WeatherLocationResponse toLocationResponse(WeatherLocation wl) {
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

    public static List<String> splitLocationNames(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        // 공백과 슬래시를 구분자로 통일하고 다중 공백은 하나로 축소
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
