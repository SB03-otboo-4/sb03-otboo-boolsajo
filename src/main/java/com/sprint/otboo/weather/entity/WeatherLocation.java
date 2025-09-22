package com.sprint.otboo.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "weather_locations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_weather_locations_lat_lng",
            columnNames = {"latitude", "longitude"}
        )
    }
)
public class WeatherLocation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "latitude", nullable = false, precision = 12, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 13, scale = 6)
    private BigDecimal longitude;

    @Min(0) @Max(500)
    @Column(name = "x", nullable = false)
    private Integer x;

    @Min(0) @Max(500)
    @Column(name = "y", nullable = false)
    private Integer y;

    @Column(name = "location_names", nullable = false, length = 255)
    private String locationNames;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (locationNames == null || locationNames.trim().isEmpty()) {
            String fallback = (x != null && y != null) ? "GRID(" + x + "," + y + ")" : "GRID";
            locationNames = fallback;
        }
    }
}
