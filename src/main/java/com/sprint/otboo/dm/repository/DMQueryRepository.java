package com.sprint.otboo.dm.repository;

import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import java.util.List;
import java.util.UUID;

public interface DMQueryRepository {

    List<DirectMessageDto> findDmPageBetween(
        UUID me, UUID other, String cursorCreatedAtIso, UUID idAfter, int limitPlusOne
    );

    long countDmBetween(UUID me, UUID other);
}
