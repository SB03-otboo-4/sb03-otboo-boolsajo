package com.sprint.otboo.weather.integration.kma.mapper;

import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KmaForecastAssembler {

    private final KmaForecastMapper mapper;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /* -------------------- public API -------------------- */

    /** KMA items → 동일 (fcstDate, fcstTime) 묶음의 슬롯 목록 */
    public List<KmaForecastMapper.Slot> toSlots(List<KmaForecastItem> items) {
        if (items == null || items.isEmpty()) return List.of();

        Map<String, List<KmaForecastItem>> byTs = items.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(i -> i.getFcstDate() + "_" + i.getFcstTime()));

        List<KmaForecastMapper.Slot> slots = new ArrayList<>(byTs.size());
        for (Map.Entry<String, List<KmaForecastItem>> e : byTs.entrySet()) {
            String key = e.getKey();            // yyyyMMdd_HHmm
            String fcstDate = key.substring(0, 8);
            String fcstTime = key.substring(9);
            slots.add(mapper.toSlot(e.getValue(), fcstDate, fcstTime));
        }
        slots.sort(Comparator.comparing(KmaForecastAssembler::instantOf));
        return slots;
    }

    /** 호환용: 단기예보 원본 없이 호출되면 강수량/최저/최고는 0 또는 null */
    public List<Weather> toWeathers(List<KmaForecastMapper.Slot> slots, WeatherLocation location) {
        return toWeathers(slots, location, List.of());
    }

    /**
     * 권장 시그니처:
     * - slots: 단기예보에서 만든 슬롯
     * - location: 위치 엔티티
     * - shortTermItems: 단기예보 원본(items) – TMN/TMX/PCP 파생용
     */
    public List<Weather> toWeathers(List<KmaForecastMapper.Slot> slots,
        WeatherLocation location,
        List<KmaForecastItem> shortTermItems) {
        if (slots == null || slots.isEmpty()) return List.of();

        // 날짜별 TMN/TMX
        Map<String, Double> dailyMin = extractDailyValue(shortTermItems, "TMN");
        Map<String, Double> dailyMax = extractDailyValue(shortTermItems, "TMX");

        // 타임스탬프별 PCP(mm)
        Map<String, Double> pcpByTs = extractPrecipAmountByTs(shortTermItems);

        List<Weather> out = new ArrayList<>(slots.size());
        for (KmaForecastMapper.Slot s : slots) {
            Instant forecastAt = instantOf(s);
            Instant forecastedAt = forecastAt.minusSeconds(30 * 60);

            Double tempC = (double) s.getTemperature();
            Double pop01 = normalizePop01(s.getPrecipitationProbability());
            Double reh   = normalizeHumidityPct(s.getHumidity());

            Double speed = safeSpeedMs(s.getWindSpeedMs());
            WindStrength ws = pickWindStrength(s.getWindSpeedMs(), s.getWindQualCode());

            String tsKey = s.getFcstDate() + "_" + s.getFcstTime();
            Double amountMm = pcpByTs.getOrDefault(tsKey, 0.0);

            Double minC = dailyMin.get(s.getFcstDate());
            Double maxC = dailyMax.get(s.getFcstDate());

            Weather w = Weather.builder()
                .location(location)
                .forecastAt(forecastAt)
                .forecastedAt(forecastedAt)
                .skyStatus(s.getSky())
                .type(s.getPrecipitation())
                .currentC(tempC)
                .probability(pop01)
                .asWord(ws)
                .currentPct(reh)
                .speedMs(speed)
                .amountMm(amountMm)
                .minC(minC)
                .maxC(maxC)
                .build();

            out.add(w);
        }
        return out;
    }

    /* -------------------- PCP / TMN/TMX helpers -------------------- */

    private static Map<String, Double> extractPrecipAmountByTs(List<KmaForecastItem> items) {
        if (items == null || items.isEmpty()) return Map.of();
        Map<String, Double> out = new HashMap<>();
        for (KmaForecastItem it : items) {
            if (it == null) continue;
            if (!"PCP".equals(it.getCategory())) continue;

            String tsKey = it.getFcstDate() + "_" + it.getFcstTime();
            Double mm = parsePrecipAmountToMm(it.getFcstValue());
            if (mm == null) continue;

            out.put(tsKey, mm);
        }
        return out;
    }

    private static Map<String, Double> extractDailyValue(List<KmaForecastItem> items, String category) {
        if (items == null || items.isEmpty()) return Map.of();
        Map<String, Double> out = new HashMap<>();
        for (KmaForecastItem it : items) {
            if (it == null) continue;
            if (!category.equals(it.getCategory())) continue;

            String date = it.getFcstDate();
            Double v = parseDoubleSafe(it.getFcstValue());
            if (date != null && v != null) out.put(date, v);
        }
        return out;
    }

    /* -------------------- parsing / normalize -------------------- */

    private static Instant instantOf(KmaForecastMapper.Slot s) {
        LocalDate d = LocalDate.parse(s.getFcstDate(), DATE);
        LocalTime t = LocalTime.parse(s.getFcstTime(), TIME);
        return ZonedDateTime.of(d, t, KST).toInstant();
    }

    private static Double normalizePop01(int popPercent) {
        if (popPercent < 0) return null;
        double p = Math.min(100, popPercent);
        return p / 100.0;
    }

    private static Double normalizeHumidityPct(int humidityPercent) {
        if (humidityPercent >= 900) return null;
        if (humidityPercent < 0) return 0.0;
        if (humidityPercent > 100) return 100.0;
        return (double) humidityPercent;
    }

    private static Double safeSpeedMs(Double v) {
        if (v == null) return 0.0;
        if (v.isNaN() || v < 0) return 0.0;
        return v;
    }

    private static WindStrength pickWindStrength(Double wsdMs, Integer qualCode) {
        if (wsdMs != null && !wsdMs.isNaN() && wsdMs >= 0) {
            return bySpeed(wsdMs);
        }
        if (qualCode != null) {
            return byQual(qualCode);
        }
        return WindStrength.WEAK;
    }

    private static WindStrength bySpeed(double ms) {
        if (ms < 4.0) return WindStrength.WEAK;
        if (ms < 9.0) return WindStrength.MODERATE;
        return WindStrength.STRONG;
    }

    private static WindStrength byQual(int code) {
        return switch (code) {
            case 1 -> WindStrength.WEAK;
            case 2 -> WindStrength.MODERATE;
            case 3 -> WindStrength.STRONG;
            default -> WindStrength.WEAK;
        };
    }

    private static Double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            double d = Double.parseDouble(s.trim());
            if (d >= 900 || d <= -900) return null;
            return d;
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    /* -------------------- 강수량(mm) 파싱 -------------------- */
    private static Double parsePrecipAmountToMm(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;
        String v = raw.trim();

        // 1) 명시적 없음
        if ("강수없음".equals(v)) return 0.0;

        // 2) "1mm 미만" 등
        if (v.contains("미만")) {
            return 0.1; // 과대계상 방지: 0.1mm로 근사
        }

        // 3) "50mm 이상" 등
        if (v.contains("이상")) {
            String num = v.replaceAll("[^0-9.]", "");
            Double base = parseDoubleSafe(num);
            return round1(clampMm(base != null ? base : 0.0));
        }

        // 4) "10~20mm" 구간값 → 중앙값
        if (v.endsWith("mm") && v.contains("~")) {
            String core = v.substring(0, v.length() - 2).trim();
            String[] parts = core.split("~");
            Double a = parseDoubleSafe(parts.length > 0 ? parts[0] : null);
            Double b = parseDoubleSafe(parts.length > 1 ? parts[1] : null);
            Double mid = (a != null && b != null) ? (a + b) / 2.0 : (a != null ? a : (b != null ? b : 0.0));
            return round1(clampMm(mid));
        }

        // 5) "12mm" 단일 값
        if (v.endsWith("mm")) {
            String core = v.substring(0, v.length() - 2).trim();
            Double n = parseDoubleSafe(core);
            return round1(clampMm(n != null ? n : 0.0));
        }

        // 6) 숫자 그대로
        Double direct = parseDoubleSafe(v);
        return round1(clampMm(direct != null ? direct : 0.0));
    }

    private static double clampMm(double mm) {
        if (mm < 0.0) return 0.0;
        if (mm > 300.0) return 300.0; // 안전 상한(극한 호우)
        return mm;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
