package com.sprint.otboo.dm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(name = "DirectMessageSendRequest", description = "DM 전송 요청",
    example = """
    {
      "receiverId": "5f1a0d5e-45a0-4c9d-a2f0-0b9c1e5a7e22",
      "content": "안녕하세요!"
    }
    """
)
public record DirectMessageSendRequest(
    @Schema(description = "수신자 ID(필수)")
    @NotNull UUID receiverId,
    @Schema(description = "메시지 내용(1~2000자)")
    @Size(min = 1, max = 2000) String content
) {}
