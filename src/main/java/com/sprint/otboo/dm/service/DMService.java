package com.sprint.otboo.dm.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import java.util.UUID;

public interface DMService {

    CursorPageResponse<DirectMessageDto> getDms(
        UUID me, UUID other, String cursor, UUID idAfter, Integer limitNullable
    );
}
