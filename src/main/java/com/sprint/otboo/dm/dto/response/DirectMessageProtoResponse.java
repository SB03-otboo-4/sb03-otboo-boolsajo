package com.sprint.otboo.dm.dto.response;

import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record DirectMessageProtoResponse(
    UUID id,
    Instant createdAt,
    UserRef sender,
    UserRef receiver,
    String content
) {
    public record UserRef(UUID userId, String name, String profileImageUrl) {}

    public static DirectMessageProtoResponse from(
        DirectMessageDto d,
        UserSummaryResponse senderSummary,
        UserSummaryResponse receiverSummary
    ) {
        return new DirectMessageProtoResponse(
            d.id(),
            d.createdAt(),
            new UserRef(d.senderId(), senderSummary.name(), senderSummary.profileImageUrl()),
            new UserRef(d.receiverId(), receiverSummary.name(), receiverSummary.profileImageUrl()),
            d.content()
        );
    }
}
