package com.sprint.otboo.dm.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DirectMessageSendRequest(
    @NotNull UUID receiverId,
    @Size(min = 1, max = 2000) String content
) {}
