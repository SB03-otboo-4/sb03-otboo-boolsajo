package com.sprint.otboo.weather.change;

import com.sprint.otboo.common.base.BaseEntity;
import com.sprint.otboo.weather.entity.WeatherLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "weather_changes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_weather_changes", columnNames = {"location_id","forecast_at","type"})
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class WeatherChange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false, foreignKey = @ForeignKey(name = "fk_weather_changes_location"))
    private WeatherLocation location;

    @Column(name = "forecast_at", nullable = false)
    private Instant forecastAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WeatherChangeType type;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;
}
