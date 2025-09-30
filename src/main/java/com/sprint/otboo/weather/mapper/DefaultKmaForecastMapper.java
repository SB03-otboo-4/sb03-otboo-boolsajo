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
            case "4": return PrecipitationType.SHOWER;
            case "5": return PrecipitationType.RAIN;
            case "6": return PrecipitationType.RAIN_SNOW;
            case "7": return PrecipitationType.SNOW;
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
        if (found.isEmpty()) return defaultValue;

        String val = found.get().getFcstValue();
        return parseIntOrDefault(val, defaultValue);
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

        // WSD는 -999 같은 센티넬이 들어오므로 가드 추가된 Double 파서 사용
        Double windSpeedMs = parseDoubleOrNull(findValue(sameTs, "WSD").orElse(null));

        Integer windQualCode = null; // 정성 바람 등급 코드(필요 시 카테고리 추가)

        return new Slot(
            fcstDate, fcstTime,
            sky, precipitation,
            temperature, humidity, precipitationProbability,
            windSpeedMs, windQualCode
        );
    }

    private Optional<String> findValue(List<KmaForecastItem> items, String category) {
        return items.stream()
            .filter(i -> category.equals(i.getCategory()))
            .map(KmaForecastItem::getFcstValue)
            .findFirst();
    }

    private static boolean isMissingNumeric(double d) {
        // KMA 결측/센티넬 범위(-999, 999, >=900, <=-900)
        return d >= 900 || d <= -900;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            int v = Integer.parseInt(value.trim());
            return isMissingNumeric(v) ? defaultValue : v;
        } catch (NumberFormatException nfe) {
            // "강수없음" 등 비정형 문자열 → 기본값
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
