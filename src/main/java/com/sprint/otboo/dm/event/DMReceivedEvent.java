package com.sprint.otboo.dm.event;

import java.util.UUID;

public record DMReceivedEvent(
    UUID dmId,
    UUID senderId,
    UUID receiverId
) {}
