package com.sprint.otboo.weather.change;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeatherChangeService {

    private final WeatherRepository weatherRepository;
    private final WeatherChangeRepository changeRepository;
    private final WeatherChangeDetector detector;
    private final WeatherChangeThresholdProperties thresholds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void process(UUID locationId, Instant forecastAt) {
        List<Weather> two = weatherRepository.findLatest2(locationId, forecastAt);
        if (two.size() < 2) return;

        Weather newer = two.get(0);
        Weather older = two.get(1);

        List<DetectedChange> detected = detector.detect(older, newer, thresholds);
        for (DetectedChange dc : detected) {
            if (!changeRepository.existsByLocationIdAndForecastAtAndType(locationId, forecastAt, dc.type())) {
                WeatherChange wc = WeatherChange.builder()
                    .location(newer.getLocation())
                    .forecastAt(forecastAt)
                    .type(dc.type())
                    .detailJson(toJson(dc.detail()))
                    .build();
                changeRepository.save(wc);
            }
        }
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { return "{}"; }
    }
}