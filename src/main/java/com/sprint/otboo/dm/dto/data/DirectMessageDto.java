package com.sprint.otboo.dm.dto.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "DirectMessageDto", description = "DM 아이템")
public record DirectMessageDto(
    @Schema(description = "메시지 ID") UUID id,
    @Schema(description = "발신자 ID") UUID senderId,
    @Schema(description = "발신자 이름") String senderName,
    @Schema(description = "발신자 프로필 이미지 URL", nullable = true) String senderProfileImageUrl,
    @Schema(description = "수신자 ID") UUID receiverId,
    @Schema(description = "수신자 이름") String receiverName,
    @Schema(description = "수신자 프로필 이미지 URL", nullable = true) String receiverProfileImageUrl,
    @Schema(description = "내용") String content,
    @Schema(description = "생성시각(ISO-8601)") Instant createdAt
) {}
