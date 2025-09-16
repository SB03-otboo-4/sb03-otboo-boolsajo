package com.sprint.otboo.feed.entity;

import com.sprint.otboo.common.base.BaseEntity;
import com.sprint.otboo.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "feed_likes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_feed_likes", columnNames = {"feed_id", "user_id"})
    }
)
@Getter
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "feed_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_feed_likes_feed")
    )
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_feed_likes_user")
    )
    private User user;
}
