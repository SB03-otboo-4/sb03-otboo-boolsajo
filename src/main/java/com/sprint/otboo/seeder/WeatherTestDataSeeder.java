package com.sprint.otboo.seeder;

import com.sprint.otboo.weather.entity.*;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local","dev"})
@Order(2)
public class WeatherTestDataSeeder implements DataSeeder {

    private final WeatherRepository weatherRepository;
    private final WeatherLocationRepository weatherLocationRepository;

    @Override
    @Transactional
    public void seed() {
        WeatherLocation location = weatherLocationRepository.findAll().stream().findFirst()
            .orElseGet(() -> weatherLocationRepository.save(
                WeatherLocation.builder()
                    .latitude(37.5)
                    .longitude(126.9780)
                    .x(60).y(127)
                    .locationNames("서울특별시 중구 태평로1가")
                    .build()));

        weatherRepository.findAll().stream().findFirst().orElseGet(() -> {
            Instant now = Instant.now();
            Weather w = weatherRepository.save(Weather.builder()
                .location(location)
                .forecastedAt(now)
                .forecastAt(now)
                .skyStatus(SkyStatus.CLEAR)
                .asWord(WindStrength.WEAK)
                .type(PrecipitationType.NONE)
                .currentC(25.0)
                .probability(0.0)
                .minC(20.0)
                .maxC(28.0)
                .amountMm(0.0)
                .speedMs(1.0)
                .currentPct(0.0)
                .comparedPct(0.0)
                .comparedC(0.0)
                .build());
            log.info("[WeatherSeeder] seeded weather id={} (loc={})", w.getId(), location.getId());
            return w;
        });
    }
}
