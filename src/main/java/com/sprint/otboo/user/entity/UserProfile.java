package com.sprint.otboo.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "x")
    private Integer x;

    @Column(name = "y")
    private Integer y;

    @Column(name = "location_names", length = 255)
    private String locationNames;

    @Column(name = "temperature_sensitivity")
    private Integer temperatureSensitivity;

    @Builder
    private UserProfile(UUID userId, User user, Gender gender, LocalDate birthDate,
        BigDecimal latitude, BigDecimal longitude, Integer x, Integer y,
        String locationNames, Integer temperatureSensitivity
    ) {
        this.userId = userId;
        this.user = user;
        this.gender = gender;
        this.birthDate = birthDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.x = x;
        this.y = y;
        this.locationNames = locationNames;
        this.temperatureSensitivity = temperatureSensitivity;
    }

    /**
     * 위치 정보 업데이트
     * */
    public void updateLocation(BigDecimal latitude, BigDecimal longitude,
        Integer x, Integer y, String locationNames) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.x = x;
        this.y = y;
        this.locationNames = locationNames;
    }

    /**
     * 온도 민감도 업데이트
     * */
    public void updateTemperatureSensitivity(Integer temperatureSensitivity) {
        this.temperatureSensitivity = temperatureSensitivity;
    }

    public void updateGender(Gender gender) {
        this.gender = gender;
    }

    public void updateBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}