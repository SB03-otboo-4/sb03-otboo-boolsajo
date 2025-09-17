package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.WeatherLocation;
import java.util.Arrays;
import java.util.List;

public class WeatherMapper {

    public static WeatherLocationResponse toLocationResponse(WeatherLocation wl) {
        List<String> names = splitLocationNames(wl.getLocationNames());
        return new WeatherLocationResponse(
            wl.getLatitude(), wl.getLongitude(), wl.getX(), wl.getY(), names
        );
    }

    public static List<String> splitLocationNames(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        // 공백과 슬래시 모두 전부 구분자 취급, 다중 공백은 하나로
        String normalized = raw.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split("[\\s/]+");
        return Arrays.stream(parts).filter(s -> !s.isBlank()).toList();
    }
}
