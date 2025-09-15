package com.sprint.otboo.feed.entity;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "feed_clothes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FeedClothes extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_feed_clothes_feed"))
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothes_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_feed_clothes_clothes"))
    private Clothes clothes;

    public static FeedClothes of(Feed feed, Clothes clothes) {
        FeedClothes feedClothes = new FeedClothes();
        feedClothes.feed = feed;
        feedClothes.clothes = clothes;
        return feedClothes;
    }
}
