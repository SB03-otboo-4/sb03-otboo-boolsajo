package com.sprint.otboo.weather.entity;

public enum SkyStatus {
    CLEAR, MOSTLY_CLOUDY, CLOUDY;

    public static SkyStatus fromSkyCode(String code) {
        if (code == null) return MOSTLY_CLOUDY;
        return switch (code) {
            case "1" -> CLEAR;
            case "3" -> MOSTLY_CLOUDY;
            case "4" -> CLOUDY;
            default -> MOSTLY_CLOUDY;
        };
    }
}