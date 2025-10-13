package com.sprint.otboo.weather.repository;

import com.sprint.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    @Query("""
        SELECT w FROM Weather w
        JOIN FETCH w.location l
        WHERE l.id = :locationId
          AND w.forecastAt BETWEEN :from AND :to
        ORDER BY w.forecastAt ASC, w.forecastedAt DESC
    """)
    List<Weather> findRangeOrderedWithLocation(
        @Param("locationId") UUID locationId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );

    default List<Weather> findRangeOrdered(UUID locationId, Instant from, Instant to) {
        return findRangeOrderedWithLocation(locationId, from, to);
    }

    // 동일 forecastAt의 이전 발표본 삭제
    @Modifying
    @Transactional
    @Query(value = """
        WITH latest AS (
          SELECT location_id, forecast_at, MAX(forecasted_at) AS keep_at
          FROM weathers
          WHERE location_id = :locationId
            AND forecast_at BETWEEN :from AND :to
          GROUP BY location_id, forecast_at
        )
        DELETE FROM weathers w
        USING latest l
        WHERE w.location_id = l.location_id
          AND w.forecast_at = l.forecast_at
          AND w.forecasted_at < l.keep_at
    """, nativeQuery = true)
    int deleteOlderVersionsInRange(
        @Param("locationId") UUID locationId,
        @Param("from") Instant from,
        @Param("to") Instant to
    );

    // 이미 지난 예보 중 오래된 데이터 삭제
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM weathers WHERE forecast_at < :cutoff", nativeQuery = true)
    int deletePastForecastsBefore(@Param("cutoff") Instant cutoff);
}
