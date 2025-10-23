package com.sprint.otboo.feed.entity;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.common.base.BaseUpdatableEntity;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.Weather;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@SuperBuilder
@Entity
@Table(name = "feeds")
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

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Builder.Default
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedClothes> feedClothes = new ArrayList<>();

    public void updateContent(String content) {
        String newContent = content == null ? null : content.trim();

        if (Objects.equals(this.content, newContent)) {
            return;
        }
        this.content = newContent;
    }

    public void addClothes(Clothes clothes) {
        FeedClothes link = FeedClothes.of(this, clothes);
        this.feedClothes.add(link);
    }

    public void increaseLikeCount() {
        this.likeCount = this.likeCount + 1;
    }

    public void softDelete() {
        this.deleted = true;
    }

    public void decreaseLikeCount() {
        this.likeCount = this.likeCount - 1;
    }
}
