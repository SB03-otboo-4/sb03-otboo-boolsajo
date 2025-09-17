package com.sprint.otboo.weather.repository;

import com.sprint.otboo.weather.entity.WeatherLocation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherLocationRepository extends JpaRepository<WeatherLocation, UUID> {

    Optional<WeatherLocation> findFirstByLatitudeAndLongitude(double latitude, double longitude);
    Optional<WeatherLocation> findFirstByXAndY(int x, int y);
}
