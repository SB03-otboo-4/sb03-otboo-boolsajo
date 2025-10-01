package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DefaultKmaForecastMapper implements KmaForecastMapper {

    private static final int DEFAULT_INT_MISSING = 999;
    private static final int DEFAULT_POP = 0;
    private static final int SENTINEL_ABS_MIN = -900;
    private static final int SENTINEL_ABS_MAX = 900;

    @Override
    public SkyStatus mapSky(String skyCode) {
        return SkyStatus.fromSkyCode(skyCode);
    }

    @Override
    public PrecipitationType mapPrecipitation(String ptyCode) {
        return PrecipitationType.fromPtyCode(ptyCode);
    }

    @Override
    public int extractInt(List<KmaForecastItem> items, String category, int defaultValue) {
        if (items == null || category == null) return defaultValue;
        return items.stream()
            .filter(Objects::nonNull)
            .filter(i -> category.equals(i.getCategory()))
            .map(KmaForecastItem::getFcstValue)
            .findFirst()
            .map(v -> parseIntOrDefault(v, defaultValue))
            .orElse(defaultValue);
    }

    @Override
    public Slot toSlot(List<KmaForecastItem> items, String fcstDate, String fcstTime) {
        List<KmaForecastItem> sameTs = items.stream()
            .filter(Objects::nonNull)
            .filter(i -> Objects.equals(fcstDate, i.getFcstDate())
                && Objects.equals(fcstTime, i.getFcstTime()))
            .toList();

        SkyStatus sky = SkyStatus.fromSkyCode(findValue(sameTs, "SKY").orElse(null));
        PrecipitationType precipitation = PrecipitationType.fromPtyCode(findValue(sameTs, "PTY").orElse(null));

        int temperature = parseIntOrDefault(findValue(sameTs, "TMP").orElse(null), DEFAULT_INT_MISSING);
        int humidity = parseIntOrDefault(findValue(sameTs, "REH").orElse(null), DEFAULT_INT_MISSING);
        int precipitationProbability = parseIntOrDefault(findValue(sameTs, "POP").orElse(null), DEFAULT_POP);

        Double windSpeedMs = parseDoubleOrNull(findValue(sameTs, "WSD").orElse(null));

        return new Slot(
            fcstDate, fcstTime,
            sky, precipitation,
            temperature, humidity, precipitationProbability,
            windSpeedMs, null
        );
    }

    private Optional<String> findValue(List<KmaForecastItem> items, String category) {
        return items.stream()
            .filter(i -> category.equals(i.getCategory()))
            .map(KmaForecastItem::getFcstValue)
            .findFirst();
    }

    private static boolean isMissingNumeric(double d) {
        return d >= SENTINEL_ABS_MAX || d <= SENTINEL_ABS_MIN;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            int v = Integer.parseInt(value.trim());
            return isMissingNumeric(v) ? defaultValue : v;
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            double d = Double.parseDouble(value.trim());
            return isMissingNumeric(d) ? null : d;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
