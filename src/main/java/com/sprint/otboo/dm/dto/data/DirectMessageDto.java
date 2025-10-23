package com.sprint.otboo.dm.dto.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "DirectMessageDto", description = "DM 아이템",
    example = """
    {
      "id": "7d9a3c69-3b8e-4a1e-8b6f-3b4d19f71dcd",
      "senderId": "2ec7b3a5-2a7e-45b2-9f44-2c8a1b8b6d11",
      "senderName": "alice",
      "senderProfileImageUrl": "https://s3.amazonaws.com/bucket/u/alice.png",
      "receiverId": "5f1a0d5e-45a0-4c9d-a2f0-0b9c1e5a7e22",
      "receiverName": "bob",
      "receiverProfileImageUrl": null,
      "content": "안녕! 오늘 저녁에 통화 가능해?",
      "createdAt": "2025-10-12T09:12:34Z"
    }
    """
)
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
