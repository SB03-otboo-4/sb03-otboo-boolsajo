package com.sprint.otboo.feed.entity;

import com.sprint.otboo.common.base.BaseUpdatableEntity;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.Weather;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@SuperBuilder
@Entity
@Table(
    name = "feeds",
    indexes = {
        @Index(name = "idx_feeds_author_created_at", columnList = "author_id, created_at"),
        @Index(name = "idx_feeds_weather", columnList = "weather_id")
    }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseUpdatableEntity {

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "comment_count", nullable = false)
    private long commentCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "author_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_feeds_author")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "weather_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_feeds_weather")
    )
    private Weather weather;
}
