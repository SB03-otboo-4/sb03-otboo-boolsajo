package com.sprint.otboo.follow.entity;

import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_follows", columnNames = {"follower_id", "followee_id"})
    }
)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @Column(name = "follower_id", nullable = false, columnDefinition = "uuid")
    private UUID followerId;

    @Column(name = "followee_id", nullable = false, columnDefinition = "uuid")
    private UUID followeeId;

    public static Follow of(UUID followerId, UUID followeeId) {
        return Follow.builder()
            .followerId(followerId)
            .followeeId(followeeId)
            .build();
    }
}
