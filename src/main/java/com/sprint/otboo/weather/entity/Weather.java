package com.sprint.otboo.weather.entity;

import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
    name = "weathers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_weathers_loc_forecast_at_forecasted_at",
            columnNames = {"location_id", "forecast_at", "forecasted_at"}
        )
    },
    indexes = {
        @Index(name = "idx_weathers_loc_forecast_at", columnList = "location_id, forecast_at"),
        @Index(name = "idx_weathers_loc_forecast_at_forecasted_at", columnList = "location_id, forecast_at, forecasted_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Weather extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "location_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_weathers_location")
    )
    private WeatherLocation location;

    @Column(name = "forecasted_at", nullable = false)
    private Instant forecastedAt;

    @Column(name = "forecast_at", nullable = false)
    private Instant forecastAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sky_status", nullable = false, length = 20)
    private SkyStatus skyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "as_word", nullable = false, length = 20)
    private WindStrength asWord;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PrecipitationType type;

    @Column(name = "speed_ms")
    private Double speedMs;

    @Column(name = "current_pct")
    private Double currentPct;

    @Column(name = "compared_pct")
    private Double comparedPct;

    @Column(name = "current_c", nullable = false)
    private Double currentC;

    @Column(name = "compared_c")
    private Double comparedC;

    @Column(name = "min_c")
    private Double minC;

    @Column(name = "max_c")
    private Double maxC;

    @Column(name = "amount_mm")
    private Double amountMm;

    @Column(name = "probability", nullable = false)
    private Double probability;
}
