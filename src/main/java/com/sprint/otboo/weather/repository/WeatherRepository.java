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
    @Query(value = """
    DELETE
    FROM weathers w
    WHERE w.location_id = :locationId
      AND w.forecast_at BETWEEN :from AND :to
      AND w.forecasted_at < (
          SELECT MAX(w2.forecasted_at)
          FROM weathers w2
          WHERE w2.location_id = w.location_id
            AND w2.forecast_at = w.forecast_at
      )
      -- 추천에서 참조 중이면 삭제하지 않음
      AND NOT EXISTS (
          SELECT 1
          FROM recommendations r
          WHERE r.weather_id = w.id
      )
    """,
        nativeQuery = true)
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

    // 특정 Weather ID로 Weather와 연관된 Location을 함께 조회
    @Query("""
    SELECT w FROM Weather w
    JOIN FETCH w.location l
    WHERE w.id = :weatherId
""")
    Optional<Weather> findByIdWithLocation(@Param("weatherId") UUID weatherId);
}
