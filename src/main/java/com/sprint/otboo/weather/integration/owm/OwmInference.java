package com.sprint.otboo.weather.integration.owm;

public final class OwmInference {

    public enum SkyStatus { CLEAR, MOSTLY_CLOUDY, CLOUDY }
    public enum PrecipitationType { NONE, RAIN, RAIN_SNOW, SNOW, SHOWER }

    private OwmInference() {}

    public static SkyStatus toSkyStatus(Integer cloudsAll, Integer weatherId) {
        // 코드 우선 보정
        if (weatherId != null) {
            if (weatherId == 800) return SkyStatus.CLEAR;
            if (weatherId == 803 || weatherId == 804) return SkyStatus.CLOUDY;
            if (weatherId == 801 || weatherId == 802) return SkyStatus.MOSTLY_CLOUDY;
        }
        int c = cloudsAll == null ? 0 : cloudsAll;
        if (c <= 10) return SkyStatus.CLEAR;
        if (c <= 70) return SkyStatus.MOSTLY_CLOUDY;
        return SkyStatus.CLOUDY;
    }

    public static PrecipitationType toPrecipType(Integer weatherId) {
        if (weatherId == null) return PrecipitationType.NONE;
        int id = weatherId;

        if (id >= 200 && id <= 232) return PrecipitationType.SHOWER;         // Thunderstorm
        if (id >= 300 && id <= 321) return PrecipitationType.RAIN;           // Drizzle
        if (id >= 500 && id <= 531) {                                        // Rain
            if (id == 511) return PrecipitationType.RAIN_SNOW;               // Freezing rain
            if (id >= 520) return PrecipitationType.SHOWER;                  // shower rain
            return PrecipitationType.RAIN;
        }
        if (id >= 600 && id <= 622) {                                        // Snow
            if (id == 611 || id == 612 || id == 613 || id == 615 || id == 616)
                return PrecipitationType.RAIN_SNOW;                          // sleet / rain&snow
            return PrecipitationType.SNOW;
        }
        // 7xx Atmosphere 등
        return PrecipitationType.NONE;
    }

    public static double toProbability(Double pop, boolean asPercent) {
        double v = pop == null ? 0d : pop;
        return asPercent ? v * 100d : v;
    }
}
