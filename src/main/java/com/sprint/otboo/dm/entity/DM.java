package com.sprint.otboo.dm.entity;

import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "direct_messages",
    indexes = {
        @Index(name = "idx_dm_sender_created", columnList = "sender_id, created_at"),
        @Index(name = "idx_dm_receiver_created", columnList = "receiver_id, created_at")
    }
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DM extends BaseEntity {

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;
}
