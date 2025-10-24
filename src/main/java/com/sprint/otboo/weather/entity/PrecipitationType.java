package com.sprint.otboo.weather.entity;

public enum PrecipitationType {
    NONE, RAIN, RAIN_SNOW, SNOW, SHOWER;

    public static PrecipitationType fromPtyCode(String code) {
        if (code == null) return NONE;
        return switch (code) {
            case "0" -> NONE;
            case "1", "5" -> RAIN;
            case "2", "6" -> RAIN_SNOW;
            case "3", "7" -> SNOW;
            case "4" -> SHOWER;
            default -> NONE;
        };
    }
}