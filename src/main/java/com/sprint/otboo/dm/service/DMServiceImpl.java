package com.sprint.otboo.dm.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.repository.DMRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DMServiceImpl implements DMService {

    private final DMRepository repository;

    @Override
    public CursorPageResponse<DirectMessageDto> getDms(
        UUID me, UUID other, String cursor, UUID idAfter, Integer limitNullable
    ) {
        int limit = (limitNullable == null) ? 20 : Math.max(1, Math.min(100, limitNullable));
        List<DirectMessageDto> list = repository.findDmPageBetween(me, other, cursor, idAfter, limit + 1);

        boolean hasNext = list.size() > limit;
        if (hasNext) list = list.subList(0, limit);

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !list.isEmpty()) {
            DirectMessageDto last = list.get(list.size() - 1);
            nextCursor = last.createdAt().toString();
            nextIdAfter = last.id().toString();
        }

        long total = repository.countDmBetween(me, other);
        return new CursorPageResponse<>(
            list, nextCursor, nextIdAfter, hasNext, total, "createdAt", "DESCENDING"
        );
    }
}
