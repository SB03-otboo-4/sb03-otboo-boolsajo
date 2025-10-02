package com.sprint.otboo.follow.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follows", uniqueConstraints = {
    @UniqueConstraint(name = "uq_follows", columnNames = {"follower_id","followee_id"})
})
public class Follow {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "follower_id", nullable = false, columnDefinition = "uuid")
    private UUID followerId;

    @Column(name = "followee_id", nullable = false, columnDefinition = "uuid")
    private UUID followeeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Follow() {}

    public Follow(UUID id, UUID followerId, UUID followeeId, Instant createdAt) {
        this.id = id;
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createdAt = createdAt;
    }

    public static Follow of(UUID followerId, UUID followeeId) {
        return new Follow(UUID.randomUUID(), followerId, followeeId, Instant.now());
    }

    public UUID getId() { return id; }
    public UUID getFollowerId() { return followerId; }
    public UUID getFolloweeId() { return followeeId; }
    public Instant getCreatedAt() { return createdAt; }
}
