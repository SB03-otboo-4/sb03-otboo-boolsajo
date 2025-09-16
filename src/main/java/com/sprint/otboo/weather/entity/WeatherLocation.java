package com.sprint.otboo.weather.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "weather_locations")
public class WeatherLocation {

    @Id
    private UUID id;
    private double latitude;
    private double longitude;
    @Column(name = "x")
    private int x;
    @Column(name = "y")
    private int y;
    @Column(name = "location_names", nullable = false)
    private String locationNames;
    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (locationNames == null) {
            locationNames = "";
        }
    }
}
