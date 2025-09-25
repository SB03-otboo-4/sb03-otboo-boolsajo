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

    @Override
    public SkyStatus mapSky(String skyCode) {
        if (skyCode == null) return SkyStatus.MOSTLY_CLOUDY;
        switch (skyCode) {
            case "1": return SkyStatus.CLEAR;
            case "3": return SkyStatus.MOSTLY_CLOUDY;
            case "4": return SkyStatus.CLOUDY;
            default:  return SkyStatus.MOSTLY_CLOUDY;
        }
    }

    @Override
    public PrecipitationType mapPrecipitation(String ptyCode) {
        if (ptyCode == null) return PrecipitationType.NONE;
        switch (ptyCode) {
            case "0": return PrecipitationType.NONE;
            case "1": return PrecipitationType.RAIN;
            case "2": return PrecipitationType.RAIN_SNOW;
            case "3": return PrecipitationType.SNOW;
            default:  return PrecipitationType.NONE;
        }
    }

    @Override
    public int extractInt(List<KmaForecastItem> items, String category, int defaultValue) {
        if (items == null || category == null) return defaultValue;
        Optional<KmaForecastItem> found = items.stream()
            .filter(Objects::nonNull)
            .filter(i -> category.equals(i.getCategory()))
            .findFirst();
        if (!found.isPresent()) return defaultValue;

        String val = found.get().getFcstValue();
        if (val == null || val.isBlank()) return defaultValue;
        try { return Integer.parseInt(val.trim()); }
        catch (NumberFormatException nfe) { return defaultValue; }
    }

    @Override
    public Slot toSlot(List<KmaForecastItem> items, String fcstDate, String fcstTime) {
        List<KmaForecastItem> sameTs = items.stream()
            .filter(Objects::nonNull)
            .filter(i -> Objects.equals(fcstDate, i.getFcstDate())
                && Objects.equals(fcstTime, i.getFcstTime()))
            .toList();

        SkyStatus sky = mapSky(findValue(sameTs, "SKY").orElse(null));
        PrecipitationType precipitation = mapPrecipitation(findValue(sameTs, "PTY").orElse(null));
        int temperature = parseIntOrDefault(findValue(sameTs, "TMP").orElse(null), 999);
        int humidity = parseIntOrDefault(findValue(sameTs, "REH").orElse(null), 999);
        int precipitationProbability = parseIntOrDefault(findValue(sameTs, "POP").orElse(null), 0);

        return new Slot(fcstDate, fcstTime, sky, precipitation, temperature, humidity, precipitationProbability);
    }

    private Optional<String> findValue(List<KmaForecastItem> items, String category) {
        return items.stream()
            .filter(i -> category.equals(i.getCategory()))
            .map(KmaForecastItem::getFcstValue)
            .findFirst();
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException nfe) { return defaultValue; }
    }
}
