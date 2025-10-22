package com.sprint.otboo.dm.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectMessageSendRequest(
    @NotNull UUID receiverId,
    @Size(min = 1, max = 2000) String content
) {}
