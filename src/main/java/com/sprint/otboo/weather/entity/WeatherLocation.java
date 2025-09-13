package com.sprint.otboo.weather.entity;

import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
    name = "weather_locations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_weather_locations_lat_lng",
            columnNames = {"latitude", "longitude"}
        )
    },
    indexes = {
        @Index(name = "idx_weather_locations_lat_lng", columnList = "latitude, longitude"),
        @Index(name = "idx_weather_locations_created_at", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class WeatherLocation extends BaseEntity {

    @Column(name = "latitude", nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false)
    private BigDecimal longitude;

    @Column(name = "x", nullable = false)
    private Integer x;

    @Column(name = "y", nullable = false)
    private Integer y;

    @Column(name = "location_names", nullable = false, length = 255)
    private String locationNames;
}
