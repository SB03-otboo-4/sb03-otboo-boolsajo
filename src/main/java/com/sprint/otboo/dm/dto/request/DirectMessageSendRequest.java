package com.sprint.otboo.dm.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(
    name = "DirectMessageSendRequest",
    description = """
        DM 전송 요청 본문.
        - senderId는 서버가 인증 컨텍스트에서 결정(클라이언트가 보내지 않음)
        - content는 1~2000자
        """,
    example = """
    {
      "receiverId": "5f1a0d5e-45a0-4c9d-a2f0-0b9c1e5a7e22",
      "content": "안녕하세요!"
    }
    """
)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectMessageSendRequest(
    @Schema(description = "수신자 ID", requiredMode = Schema.RequiredMode.REQUIRED,
        example = "5f1a0d5e-45a0-4c9d-a2f0-0b9c1e5a7e22")
    @NotNull UUID receiverId,

    @Schema(description = "메시지 내용(1~2000자)", maxLength = 2000, example = "안녕하세요!")
    @Size(min = 1, max = 2000) String content
) {}
