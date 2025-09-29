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
        List<Weather> list = new ArrayList<>();
        for (KmaForecastMapper.Slot s : slots) {
            Instant forecastAt = instantOf(s);
            Instant forecastedAt = forecastAt.minusSeconds(30 * 60);

            Weather w = Weather.builder()
                .location(location)
                .forecastAt(forecastAt)
                .forecastedAt(forecastedAt)
                .skyStatus(s.getSky())
                .type(s.getPrecipitation())
                .currentC((double) s.getTemperature())
                .probability(((double) s.getPrecipitationProbability()) / 100.0)
                .asWord(WindStrength.MODERATE)
                .currentPct((double) s.getHumidity())
                .speedMs(0.0)
                .build();
            list.add(w);
        }
        return list;
    }

    private static Instant instantOf(KmaForecastMapper.Slot s) {
        LocalDate d = LocalDate.parse(s.getFcstDate(), DATE);
        LocalTime t = LocalTime.parse(s.getFcstTime(), TIME);
        return ZonedDateTime.of(d, t, ZoneId.of("Asia/Seoul")).toInstant();
    }
}
