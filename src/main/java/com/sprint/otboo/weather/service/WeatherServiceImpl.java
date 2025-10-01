package com.sprint.otboo.weather.service;

import com.sprint.otboo.weather.dto.data.WeatherDto;
import com.sprint.otboo.weather.dto.data.WeatherSummaryDto;
import com.sprint.otboo.weather.dto.response.WeatherLocationResponse;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.integration.kma.KmaRequestBuilder;
import com.sprint.otboo.weather.integration.kma.client.KmaShortTermForecastClient;
import com.sprint.otboo.weather.integration.kma.dto.KmaForecastResponse;
import com.sprint.otboo.weather.mapper.KmaForecastAssembler;
import com.sprint.otboo.weather.mapper.KmaForecastMapper.Slot;
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
import java.util.TreeMap;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final KmaRequestBuilder kmaRequestBuilder;
    private final KmaShortTermForecastClient kmaClient;
    private final KmaForecastAssembler kmaAssembler;
    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional
    public List<WeatherDto> getWeather(Double latitude, Double longitude) {

        // 1) 위치 조회
        WeatherLocationResponse locDto = locationQueryService.getWeatherLocation(latitude, longitude);
        WeatherLocation location = resolveLocationEntity(locDto);

        // 2) KMA 요청 파라미터
        Instant now = Instant.now();
        Map<String, String> params = kmaRequestBuilder.toParams(locDto.latitude(), locDto.longitude(), now);

        try {
            // 3) KMA 호출 → 슬롯/스냅샷 변환
            KmaForecastResponse response = kmaClient.getVilageFcst(params);
            List<Slot> slots = kmaAssembler.toSlots(response.getItems());
            List<Weather> rawSnapshots = kmaAssembler.toWeathers(slots, location);

            // 4) 중복 방지 저장(이미 있으면 기존 레코드 반환)
            List<Weather> persisted = persistDedup(location.getId(), rawSnapshots);

            // 4-1) 미래 5일 구간을 저장소에서 추가 로드하여 데이터 폭 보강
            List<Weather> material = withFiveDayRangeFromStore(location.getId(), persisted);

            // 5) 일자별 같은 시각 대표 + 그날의 min/max + 전일 같은 시각 ΔT(comparedC) + Δ습도(comparedPct)
            List<Weather> dailyRepresentatives = aggregatePerDaySameHour(material);

            // 6) 최대 5일만 반환
            List<Weather> top5 = dailyRepresentatives.size() > 5
                ? dailyRepresentatives.subList(0, 5)
                : dailyRepresentatives;

            // 7) 응답 매핑
            return top5.stream().map(weatherMapper::toWeatherDto).toList();

        } catch (RuntimeException upstream) {
            // 폴백: 캐시에서 복원 후 동일 집계
            log.warn("KMA upstream error. Falling back to cached data. x={}, y={}",
                locDto.x(), locDto.y(), upstream);

            List<Weather> material = withFiveDayRangeFromStore(location.getId(), List.of());
            if (!material.isEmpty()) {
                List<Weather> dailyRepresentatives = aggregatePerDaySameHour(material);
                List<Weather> top5 = dailyRepresentatives.size() > 5
                    ? dailyRepresentatives.subList(0, 5)
                    : dailyRepresentatives;
                return top5.stream().map(weatherMapper::toWeatherDto).collect(Collectors.toList());
            }
            throw upstream;
        }
    }

    /** 오늘 00:00(KST) ~ +4일 23:59:59(KST) 구간을 저장소에서 읽어 현재 리스트에 합친다. */
    private List<Weather> withFiveDayRangeFromStore(UUID locationId, List<Weather> base) {
        LocalDate todayKst = Instant.now().atZone(KST).toLocalDate();
        ZonedDateTime fromZdt = todayKst.atStartOfDay(KST);
        ZonedDateTime toZdt = todayKst.plusDays(4).atTime(23, 59, 59).atZone(KST);

        Instant from = fromZdt.toInstant();
        Instant to = toZdt.toInstant();

        List<Weather> range = weatherRepository.findRangeOrdered(locationId, from, to);

        // base + range 합치고 forecastAt ASC, forecastedAt DESC 정렬 가정하에 최신값 우선 유지
        List<Weather> merged = new ArrayList<>(base.size() + range.size());
        merged.addAll(base);
        merged.addAll(range);

        // 같은 forecastAt 내에서 첫 레코드가 최신이 되도록 정제
        return pickLatestPerForecastAt(
            merged.stream()
                .sorted(Comparator
                    .comparing(Weather::getForecastAt)
                    .thenComparing(Weather::getForecastedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList()
        );
    }

    /**
     * ① KST 일자별 그룹
     * ② 요청 시각(HH:00) 정확히 일치 우선, 없으면 가장 근접한 슬롯을 대표로
     * ③ 그 날 전체 슬롯으로 min/max 계산 (temperature)
     * ④ 전일 대표와 같은 시각 비교로 ΔT(comparedC) + Δ습도(comparedPct) 계산
     */
    private List<Weather> aggregatePerDaySameHour(List<Weather> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return List.of();

        LocalDate todayKst = Instant.now().atZone(KST).toLocalDate();
        int targetHour = Instant.now().atZone(KST).getHour();
        LocalTime targetTime = LocalTime.of(targetHour, 0);

        // 오늘 이후만 대상으로 정렬
        List<Weather> futureOrToday = snapshots.stream()
            .filter(w -> w.getForecastAt() != null)
            .filter(w -> !w.getForecastAt().atZone(KST).toLocalDate().isBefore(todayKst))
            .sorted(Comparator.comparing(Weather::getForecastAt))
            .toList();
        if (futureOrToday.isEmpty()) return List.of();

        // 날짜별 그룹(TreeMap: 날짜 오름차순)
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

            // min/max: 그날 모든 슬롯의 currentC
            DoubleSummaryStatistics stat = ws.stream()
                .map(Weather::getCurrentC)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            double min = stat.getCount() > 0 ? stat.getMin() : 0.0;
            double max = stat.getCount() > 0 ? stat.getMax() : 0.0;
            minMaxByDay.put(day, new double[]{min, max});

            // 대표 슬롯: HH:00 정확 매칭 우선, 없으면 가장 근접
            Weather exact = ws.stream()
                .filter(w -> {
                    LocalTime lt = w.getForecastAt().atZone(KST).toLocalTime();
                    return lt.getHour() == targetHour && lt.getMinute() == 0;
                })
                .findFirst()
                .orElse(null);

            Weather rep = (exact != null) ? exact
                : ws.stream()
                    .min(Comparator.comparingLong(w -> {
                        LocalTime lt = w.getForecastAt().atZone(KST).toLocalTime();
                        return Math.abs(Duration.between(lt, targetTime).toMinutes());
                    }))
                    .orElse(ws.get(0));

            repByDay.put(day, rep);
        }

        // 전일 대표와 비교(같은 시각)하여 ΔT(comparedC) + Δ습도(comparedPct) + min/max 주입
        List<LocalDate> days = new ArrayList<>(repByDay.keySet());
        List<Weather> result = new ArrayList<>(days.size());

        Double prevTemp = null;
        Double prevHumidity = null; // ← 습도 비교를 위한 이전일 대표 습도(%)

        for (LocalDate day : days) {
            Weather rep = repByDay.get(day);
            double[] mm = minMaxByDay.get(day);

            // 온도 비교
            Double currentTemp = rep.getCurrentC();
            Double comparedTemp = (prevTemp != null && currentTemp != null)
                ? currentTemp - prevTemp : null;
            prevTemp = currentTemp;

            // 습도 비교(percentage point 변화량)
            Double currentHumidity = rep.getCurrentPct(); // 현재 대표 슬롯의 습도(%)
            Double comparedHumidity = (prevHumidity != null && currentHumidity != null)
                ? currentHumidity - prevHumidity : null;
            prevHumidity = currentHumidity;

            Weather enriched = rep.toBuilder()
                .location(rep.getLocation())
                .id(rep.getId())
                .minC(mm[0])
                .maxC(mm[1])
                .comparedC(comparedTemp)
                .comparedPct(comparedHumidity)
                .build();

            result.add(enriched);
        }

        return result;
    }

    /** 같은 forecastAt 내에서 첫 번째 레코드가 "가장 최신 예보" */
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

    /** 중복 확인 후 저장하고, 저장된 엔티티(이미 존재하면 기존 레코드)를 반환 리스트에 담는다. */
    private List<Weather> persistDedup(UUID locationId, List<Weather> snapshots) {
        List<Weather> persisted = new ArrayList<>(snapshots.size());
        for (Weather w : snapshots) {
            Optional<Weather> existing = weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
                locationId, w.getForecastAt(), w.getForecastedAt()
            );

            Weather saved = existing.orElseGet(() -> {
                try {
                    return weatherRepository.save(w);
                } catch (DataIntegrityViolationException e) {
                    // 동시 저장 경합 발생 시 재조회
                    return weatherRepository.findByLocationIdAndForecastAtAndForecastedAt(
                        locationId, w.getForecastAt(), w.getForecastedAt()
                    ).orElse(w);
                }
            });

            persisted.add(saved);
        }
        return persisted;
    }

    private WeatherLocation resolveLocationEntity(WeatherLocationResponse dto) {
        Optional<WeatherLocation> byXY = locationRepository.findFirstByXAndY(dto.x(), dto.y());
        if (byXY.isPresent()) return byXY.get();

        return locationRepository.findFirstByLatitudeAndLongitude(
            BigDecimal.valueOf(dto.latitude()),
            BigDecimal.valueOf(dto.longitude())
        ).orElseThrow(() -> new IllegalStateException("WeatherLocation entity not found"));
    }
}
