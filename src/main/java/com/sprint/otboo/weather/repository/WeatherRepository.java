package com.sprint.otboo.weather.repository;

import com.sprint.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    Optional<Weather> findByLocationIdAndForecastAtAndForecastedAt(
        UUID locationId, Instant forecastAt, Instant forecastedAt
    );

    List<Weather> findTop2ByLocationIdAndForecastAtOrderByForecastedAtDesc(
        UUID locationId, Instant forecastAt
    );

    List<Weather> findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
        UUID locationId, Instant from, Instant to
    );

    default List<Weather> findLatest2(UUID locationId, Instant forecastAt) {
        return findTop2ByLocationIdAndForecastAtOrderByForecastedAtDesc(locationId, forecastAt);
    }

    default List<Weather> findRangeOrdered(UUID locationId, Instant from, Instant to) {
        return findAllByLocationIdAndForecastAtBetweenOrderByForecastAtAscForecastedAtDesc(
            locationId, from, to
        );
    }
}
