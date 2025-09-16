package com.sprint.otboo.weather.repository;

import com.sprint.otboo.weather.entity.WeatherLocation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherLocationRepository extends JpaRepository<WeatherLocation, UUID> {

}
