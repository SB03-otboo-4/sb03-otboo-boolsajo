package com.sprint.otboo.weather.change;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherChangeRepository extends JpaRepository<WeatherChange, UUID> {

    boolean existsByLocationIdAndForecastAtAndType(UUID locationId, Instant forecastAt, WeatherChangeType type);
}
