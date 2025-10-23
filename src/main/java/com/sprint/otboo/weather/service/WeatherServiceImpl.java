package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.data.WeatherDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient;
import com.sprint.otboo.weather.integration.spi.WeatherDataClient.CollectedForecast;
import com.sprint.otboo.weather.mapper.OwmForecastAssembler;
import com.sprint.otboo.weather.mapper.WeatherMapper;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final WeatherLocationQueryService locationQueryService;
    private final WeatherLocationRepository locationRepository;

    // ⬇️ 교체: KMA 의존 제거 → 공통 수집기 + OWM 어셈블러
    private final WeatherDataClient weatherDataClient;
    private final OwmForecastAssembler owmAssembler;
    private final Locale owmDefaultLocale; // WeatherOwmConfig에서 Bean으로 제공 중

    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional
    public List<WeatherDto> getWeather(Double latitude, Double longitude) {

        // 위치 정보 조회
        WeatherLocationResponse locDto = locationQueryService.getWeatherLocation(latitude, longitude);
        WeatherLocation location = resolveLocationEntity(locDto);

        try {
            // OWM 수집 (SPI)
            List<CollectedForecast> collected = weatherDataClient.fetch(
                locDto.latitude(), locDto.longitude(), owmDefaultLocale
            );

            if (collected == null || collected.isEmpty()) {
                log.warn("OWM returned empty forecast. Falling back to cached data. lat={}, lon={}",
                    locDto.latitude(), locDto.longitude());
                List<Weather> material = withFiveDayRangeFromStore(location.getId(), List.of());
                return toTop5Dtos(material);
            }

            // 엔티티 변환 (발표시각 대용으로 수집시각 사용)
            Instant ingestedAt = Instant.now();
            List<Weather> rawSnapshots = collected.stream()
                .map(cf -> owmAssembler.toEntity(location, cf, ingestedAt))
                .toList();

            // 중복 방지 저장
            List<Weather> persisted = persistDedup(location.getId(), rawSnapshots);

            // 어제~+4일 구간 로드(Δ 계산 위해 전일 포함)
            List<Weather> material = withFiveDayRangeFromStore(location.getId(), persisted);

            // 일자별 대표 및 ΔT/Δ습도 계산 → 최대 5일 반환
            return toTop5Dtos(material);

        } catch (RuntimeException e) {
            log.warn("OWM upstream error. Falling back to cached data. lat={}, lon={}",
                locDto.latitude(), locDto.longitude(), e);

            List<Weather> material = withFiveDayRangeFromStore(location.getId(), List.of());
            if (!material.isEmpty()) {
                return toTop5Dtos(material);
            }
            throw e;
        }
    }

    /** 어제 00:00(KST) ~ +4일 23:59:59(KST) 구간 캐시 + 최신본만 선별 */
    private List<Weather> withFiveDayRangeFromStore(UUID locationId, List<Weather> base) {
        LocalDate todayKst = Instant.now().atZone(KST).toLocalDate();
        ZonedDateTime fromZdt = todayKst.minusDays(1).atStartOfDay(KST);
        ZonedDateTime toZdt = todayKst.plusDays(4).atTime(23, 59, 59).atZone(KST);

        Instant from = fromZdt.toInstant();
        Instant to = toZdt.toInstant();

        List<Weather> range = weatherRepository.findRangeOrdered(locationId, from, to);

        List<Weather> merged = new ArrayList<>(base.size() + range.size());
        merged.addAll(base);
        merged.addAll(range);

        return pickLatestPerForecastAt(
            merged.stream()
                .sorted(Comparator
                    .comparing(Weather::getForecastAt)
                    .thenComparing(Weather::getForecastedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList()
        );
    }

    /** 일자별 대표 + ΔT, Δ습도 계산 */
    private List<Weather> aggregatePerDaySameHour(List<Weather> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return List.of();

        LocalDate todayKst = Instant.now().atZone(KST).toLocalDate();
        int targetHour = Instant.now().atZone(KST).getHour();
        LocalTime targetTime = LocalTime.of(targetHour, 0);

        List<Weather> futureOrToday = snapshots.stream()
            .filter(w -> w.getForecastAt() != null)
            .filter(w -> !w.getForecastAt().atZone(KST).toLocalDate().isBefore(todayKst.minusDays(1)))
            .sorted(Comparator.comparing(Weather::getForecastAt))
            .toList();
        if (futureOrToday.isEmpty()) return List.of();

        Map<LocalDate, List<Weather>> byDay = futureOrToday.stream().collect(Collectors.groupingBy(
            w -> w.getForecastAt().atZone(KST).toLocalDate(),
            TreeMap::new,
            Collectors.toList()
        ));

        Map<LocalDate, double[]> minMaxByDay = new LinkedHashMap<>();
        Map<LocalDate, Weather> repByDay = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, List<Weather>> e : byDay.entrySet()) {
            LocalDate day = e.getKey();
            List<Weather> ws = e.getValue();

            DoubleSummaryStatistics stat = ws.stream()
                .map(Weather::getCurrentC)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            double min = stat.getCount() > 0 ? stat.getMin() : 0.0;
            double max = stat.getCount() > 0 ? stat.getMax() : 0.0;
            minMaxByDay.put(day, new double[]{min, max});

            Weather exact = ws.stream()
                .filter(w -> {
                    LocalTime lt = w.getForecastAt().atZone(KST).toLocalTime();
                    return lt.getHour() == targetHour && lt.getMinute() == 0;
                })
                .findFirst().orElse(null);

            Weather rep = (exact != null) ? exact
                : ws.stream()
                    .min(Comparator.comparingLong(w -> {
                        LocalTime lt = w.getForecastAt().atZone(KST).toLocalTime();
                        return Math.abs(Duration.between(lt, targetTime).toMinutes());
                    }))
                    .orElse(ws.get(0));

            repByDay.put(day, rep);
        }

        List<LocalDate> days = new ArrayList<>(repByDay.keySet());
        List<Weather> result = new ArrayList<>(days.size());

        Double prevTemp = null;
        Double prevHumidity = null;

        for (LocalDate day : days) {
            Weather rep = repByDay.get(day);
            double[] mm = minMaxByDay.get(day);

            Double currentTemp = rep.getCurrentC();
            Double comparedTemp = (prevTemp != null && currentTemp != null)
                ? currentTemp - prevTemp : null;
            prevTemp = currentTemp;

            Double currentHumidity = rep.getCurrentPct();
            Double comparedHumidity = (prevHumidity != null && currentHumidity != null)
                ? currentHumidity - prevHumidity : null;
            prevHumidity = currentHumidity;

            Weather enriched = rep.toBuilder()
                .minC(mm[0])
                .maxC(mm[1])
                .comparedC(comparedTemp)
                .comparedPct(comparedHumidity)
                .build();

            result.add(enriched);
        }

        return result;
    }

    private List<WeatherDto> toTop5Dtos(List<Weather> material) {
        List<Weather> dailyRepresentatives = aggregatePerDaySameHour(material);
        List<Weather> top5 = dailyRepresentatives.size() > 5
            ? dailyRepresentatives.subList(0, 5)
            : dailyRepresentatives;
        return top5.stream().map(weatherMapper::toWeatherDto).toList();
    }

    private static List<Weather> pickLatestPerForecastAt(List<Weather> ordered) {
        List<Weather> result = new ArrayList<>();
        Instant currentKey = null;
        for (Weather w : ordered) {
            if (!Objects.equals(currentKey, w.getForecastAt())) {
                result.add(w);
                currentKey = w.getForecastAt();
            }
        }
        return result;
    }

    private List<Weather> persistDedup(UUID locationId, List<Weather> snapshots) {
        if (snapshots.isEmpty()) return List.of();

        Instant minAt = snapshots.stream()
            .map(Weather::getForecastAt)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(Instant.now());

        Instant maxAt = snapshots.stream()
            .map(Weather::getForecastAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(Instant.now());

        List<Weather> existed = weatherRepository
            .findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(locationId, minAt, maxAt);

        Set<String> existedKeys = existed.stream()
            .map(w -> key(w.getForecastAt(), w.getForecastedAt()))
            .collect(Collectors.toSet());

        List<Weather> toSave = snapshots.stream()
            .filter(w -> !existedKeys.contains(key(w.getForecastAt(), w.getForecastedAt())))
            .toList();

        if (!toSave.isEmpty()) {
            try {
                weatherRepository.saveAll(toSave);
                // 동일 forecastAt 범위에서 최신 발표본만 남기기
                weatherRepository.deleteOlderVersionsInRange(locationId, minAt, maxAt);
            } catch (DataIntegrityViolationException e) {
                log.warn("Concurrent insert detected during persistDedup: {}", e.getMessage());
            }
        }

        List<Weather> persisted = new ArrayList<>(existed.size() + toSave.size());
        persisted.addAll(existed);
        persisted.addAll(toSave);
        return persisted;
    }

    private static String key(Instant forecastAt, Instant forecastedAt) {
        return forecastAt.toString() + "|" + (forecastedAt != null ? forecastedAt.toString() : "");
    }

    private WeatherLocation resolveLocationEntity(WeatherLocationResponse dto) {
        return locationRepository.findFirstByXAndY(dto.x(), dto.y())
            .orElseGet(() -> locationRepository.findFirstByLatitudeAndLongitude(
                BigDecimal.valueOf(dto.latitude()),
                BigDecimal.valueOf(dto.longitude())
            ).orElseThrow(() -> new IllegalStateException("WeatherLocation entity not found")));
    }
}
