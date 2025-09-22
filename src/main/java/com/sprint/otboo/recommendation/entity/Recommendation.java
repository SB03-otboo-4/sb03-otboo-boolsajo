package com.sprint.otboo.recommendation.entity;

import com.sprint.otboo.common.base.BaseEntity;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.Weather;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "recommendations")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Recommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_recommendations_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weather_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_recommendations_weather"))
    private Weather weather;

    @Builder.Default
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecommendationClothes> recommendationClothes = new ArrayList<>();

    public void addRecommendationClothes(RecommendationClothes rc) {
        recommendationClothes.add(rc);
        rc.setRecommendation(this);
    }

}
