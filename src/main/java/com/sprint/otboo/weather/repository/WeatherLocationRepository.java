package com.sprint.otboo.weather.repository;

import com.sprint.otboo.weather.entity.WeatherLocation;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherLocationRepository extends JpaRepository<WeatherLocation, UUID> {

    Optional<WeatherLocation> findFirstByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);
    Optional<WeatherLocation> findFirstByXAndY(int x, int y);
}
