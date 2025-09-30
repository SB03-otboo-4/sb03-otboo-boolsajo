package com.sprint.otboo.weather.mapper;

import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastItem;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KmaForecastAssembler {

    private final KmaForecastMapper mapper;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public List<KmaForecastMapper.Slot> toSlots(List<KmaForecastItem> items) {
        if (items == null || items.isEmpty()) return List.of();

        Map<String, List<KmaForecastItem>> byTs = items.stream()
            .collect(Collectors.groupingBy(i -> i.getFcstDate() + "_" + i.getFcstTime()));

        List<KmaForecastMapper.Slot> slots = new ArrayList<>();
        for (Map.Entry<String, List<KmaForecastItem>> e : byTs.entrySet()) {
            String key = e.getKey();
            String fcstDate = key.substring(0, 8);
            String fcstTime = key.substring(9);
            slots.add(mapper.toSlot(e.getValue(), fcstDate, fcstTime));
        }
        slots.sort(Comparator.comparing(KmaForecastAssembler::instantOf));
        return slots;
    }

    public List<Weather> toWeathers(List<KmaForecastMapper.Slot> slots, WeatherLocation location) {
        if (slots == null || slots.isEmpty()) return List.of();

        List<Weather> list = new ArrayList<>(slots.size());
        for (KmaForecastMapper.Slot s : slots) {
            Instant forecastAt = instantOf(s);
            // base_time을 받지 않으므로, 직전 발표 기준으로 30분 전으로 근사
            Instant forecastedAt = forecastAt.minusSeconds(30 * 60);

            Double tempC = (double) s.getTemperature();
            Double pop01 = normalizePop01(s.getPrecipitationProbability()); // 0~1 저장
            Double reh   = normalizeHumidityPct(s.getHumidity());           // 0~100 or null

            Double speed = safeSpeedMs(s.getWindSpeedMs());
            WindStrength ws = pickWindStrength(s.getWindSpeedMs(), s.getWindQualCode());

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
                .build();

            list.add(w);
        }
        return list;
    }

    private static Instant instantOf(KmaForecastMapper.Slot s) {
        LocalDate d = LocalDate.parse(s.getFcstDate(), DATE);
        LocalTime t = LocalTime.parse(s.getFcstTime(), TIME);
        return ZonedDateTime.of(d, t, KST).toInstant();
    }

    /** 강수확률(%) → 0~1 저장. 범위 밖은 클램프, 음수(결측 추정)는 null */
    private static Double normalizePop01(int popPercent) {
        if (popPercent < 0) return null;
        double p = popPercent;
        if (p > 100) p = 100;
        return p / 100.0;
    }

    /** 상대습도(%) 0~100 유지. 900급 센티넬은 null */
    private static Double normalizeHumidityPct(int humidityPercent) {
        if (humidityPercent >= 900) return null;
        if (humidityPercent < 0) return 0.0;
        if (humidityPercent > 100) return 100.0;
        return (double) humidityPercent;
    }

    /** 풍속 값 방어: null/NaN/음수 → 0.0 */
    private static Double safeSpeedMs(Double v) {
        if (v == null) return 0.0;
        if (v.isNaN() || v < 0) return 0.0;
        return v;
    }

    /** 바람 등급 산정: 풍속(m/s) 우선, 없으면 정성코드(1~3) 사용 */
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
}
